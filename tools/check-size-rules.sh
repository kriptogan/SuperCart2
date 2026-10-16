#!/usr/bin/env bash
# Checks the size rules from .claude/coding-rules.md for Kotlin sources under app/src.
#   - files:     <= 300 lines of code (blank and comment-only lines excluded)
#   - functions: <= 6 body lines (production code only; test sources are exempt)
# Usage: bash tools/check-size-rules.sh [path...]   (default: app/src)
# Exit code 1 when any rule is broken.
cd "$(dirname "$0")/.." || exit 2
paths=("$@")
[ ${#paths[@]} -eq 0 ] && paths=(app/src)

find "${paths[@]}" -name '*.kt' -print0 | xargs -0 perl -e '
use strict; use warnings;
my ($MAX_FILE, $MAX_FUN) = ($ENV{MAX_FILE} // 300, $ENV{MAX_FUN} // 6);
my $violations = 0;

for my $file (@ARGV) {
    open my $fh, "<", $file or die "$file: $!";
    my @raw = <$fh>; close $fh;
    my @lines = clean(@raw);
    my $is_test = $file =~ m{/src/test/|/src/androidTest/};

    my $loc = grep { /\S/ } @lines;
    if ($loc > $MAX_FILE) { print "FILE $file: $loc lines of code (max $MAX_FILE)\n"; $violations++; }
    next if $is_test;

    for my $i (0 .. $#lines) {
        next unless $lines[$i] =~ /(?:^|[^\w.])fun\s+(?:<[^>]*>\s*)?([\w.`<>, ]*?)\s*\(/;
        my $name = $1; $name =~ s/.*\.//;
        my $body = body_lines(\@lines, $i);
        if (defined $body && $body > $MAX_FUN) {
            printf "FUN  %s:%d %s(): %d body lines (max %d)\n", $file, $i + 1, $name, $body, $MAX_FUN;
            $violations++;
        }
    }
}
print $violations ? "\n$violations violation(s)\n" : "OK: no size-rule violations\n";
exit($violations ? 1 : 0);

# Strip comments and string/char literal contents so brackets can be counted reliably.
sub clean {
    my @out; my $in_block = 0; my $in_raw = 0;
    for my $l (@_) {
        my $s = $l; chomp $s; my $r = "";
        while (length $s) {
            if ($in_block) { if ($s =~ s{^.*?\*/}{}) { $in_block = 0 } else { $s = "" } next }
            if ($in_raw)   { if ($s =~ s{^.*?"""}{}) { $in_raw = 0; $r .= q("") } else { $s = "" } next }
            if ($s =~ s{^/\*}{})          { $in_block = 1; next }
            if ($s =~ m{^//})             { $s = ""; next }
            if ($s =~ s{^"""}{})          { $in_raw = 1; next }
            if ($s =~ s{^"(?:[^"\\]|\\.)*"}{}) { $r .= q(""); next }
            if ($s =~ s{^\x27(?:[^\x27\\]|\\.)+\x27}{}) { $r .= q(\x27\x27); next }
            $r .= substr($s, 0, 1, "");
        }
        push @out, $r;
    }
    return @out;
}

sub depth_delta { my $t = shift; return ($t =~ tr/({[//) - ($t =~ tr/)}]//); }

# Returns the number of body lines of the function declared on line $i, or undef if it has no body.
sub body_lines {
    my ($lines, $i) = @_;
    my $depth = 0; my ($kind, $start_line, $rest);
    # 1. find the body marker ("{" or "=") at depth 0, after the parameter list
    LINE: for (my $j = $i; $j < @$lines; $j++) {
        my $text = $lines->[$j];
        if ($j == $i) { $text =~ s/^.*?\bfun\b//; }
        for my $pos (0 .. length($text) - 1) {
            my $c = substr($text, $pos, 1);
            if ($c eq "(" || $c eq "[") { $depth++ }
            elsif ($c eq ")" || $c eq "]") { $depth-- }
            elsif ($depth == 0 && $c eq "{") { $kind = "block"; $start_line = $j; $rest = substr($text, $pos + 1); last LINE }
            elsif ($depth == 0 && $c eq "=" && substr($text, $pos, 2) !~ /^==/ && ($pos == 0 || substr($text, $pos - 1, 1) !~ /[=!<>]/)) {
                $kind = "expr"; $start_line = $j; $rest = substr($text, $pos + 1); last LINE
            }
        }
        return undef if $depth == 0 && $j + 1 < @$lines && $lines->[$j + 1] !~ /^\s*[{=:)]/ && $text !~ /[,(:]\s*$/;
    }
    return undef unless $kind;

    my $count = 0;
    if ($kind eq "block") {
        my $d = 1;
        my $first = $rest;
        $d += depth_delta($first);
        if ($d <= 0) { return ($first =~ /\S.*\}/) ? 1 : 0 }
        $count++ if $first =~ /\S/;
        for (my $j = $start_line + 1; $j < @$lines; $j++) {
            my $t = $lines->[$j];
            $d += depth_delta($t);
            if ($d <= 0) { (my $before = $t) =~ s/\}[^}]*$//; $count++ if $before =~ /\S/; last }
            $count++ if $t =~ /\S/;
        }
    } else {
        my $d = depth_delta($rest);
        $count++ if $rest =~ /\S/;
        my $j = $start_line;
        while (1) {
            my $cur = $lines->[$j];
            my $next_j = $j + 1; $next_j++ while $next_j < @$lines && $lines->[$next_j] !~ /\S/;
            last if $next_j >= @$lines;
            my $next = $lines->[$next_j];
            my $continues = $d > 0 || $cur =~ /(?:[=+\-*\/,(.{:]|->|&&|\|\||\?:)\s*$/ || ($j == $start_line && $rest !~ /\S/)
                || $next =~ /^\s*(?:\.|\?\.|\?:|&&|\|\||\+|-(?!>)|\*|\/)/;
            last unless $continues;
            $j = $next_j; $d += depth_delta($next); $count++;
        }
    }
    return $count;
}
'
