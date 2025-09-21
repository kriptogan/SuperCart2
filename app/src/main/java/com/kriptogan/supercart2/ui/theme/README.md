# SuperCart Design System

This design system is based on the patterns and styles used in the original SuperCart project. It provides consistent, reusable components and styles for building the SuperCart2 application.

## Color Palette

### Primary Colors
- `SuperCartGreen` (#4CAF50) - Primary action color for buttons, links, and important elements
- `SuperCartBlue` (#2196F3) - Secondary action color for secondary buttons and actions
- `SuperCartLightGreen` (#E8F5E8) - Light background for cards and sections

### Neutral Colors
- `SuperCartWhite` (#FFFFFF) - Primary background and text on dark backgrounds
- `SuperCartBlack` (#000000) - Primary text color
- `SuperCartGray` (#BDBDBD) - Border color for input fields
- `SuperCartDarkGray` (#757575) - Secondary text color

## Typography

The typography system follows Material Design 3 principles with custom sizing:

### Headlines
- `headlineLarge` - 32sp, Bold - Main page titles
- `headlineMedium` - 28sp, Bold - Section headers
- `headlineSmall` - 24sp, Bold - Subsection headers

### Titles
- `titleLarge` - 22sp, Medium - Card titles
- `titleMedium` - 18sp, Medium - Button text, important labels
- `titleSmall` - 16sp, Medium - Secondary labels

### Body Text
- `bodyLarge` - 16sp, Normal - Primary body text
- `bodyMedium` - 14sp, Normal - Secondary body text
- `bodySmall` - 12sp, Normal - Captions, small text

### Labels
- `labelLarge` - 14sp, Medium - Form labels
- `labelMedium` - 12sp, Medium - Button labels
- `labelSmall` - 11sp, Medium - Small labels

## Spacing System

Consistent spacing values for margins, padding, and gaps:
- `xs` - 4dp - Minimal spacing
- `sm` - 8dp - Small spacing
- `md` - 16dp - Medium spacing (default)
- `lg` - 24dp - Large spacing
- `xl` - 32dp - Extra large spacing
- `xxl` - 48dp - Icon button size

## Shapes

- `small` - 8dp radius - Input fields, small elements
- `medium` - 12dp radius - Cards, buttons
- `large` - 16dp radius - Large cards, sections
- `circle` - Perfect circle - Icon buttons, avatars

## Component Library

### Buttons
- `SuperCartPrimaryButton` - Green button for primary actions
- `SuperCartSecondaryButton` - Blue button for secondary actions
- `SuperCartOutlineButton` - Outlined button for tertiary actions

### Cards
- `SuperCartElevatedCard` - Standard elevated card with white background
- `SuperCartLightGreenCard` - Light green card for special sections

### Input Fields
- `SuperCartOutlinedTextField` - Standard single-line input field
- `SuperCartMultilineTextField` - Multi-line text area

## Usage Examples

### Basic Button
```kotlin
SuperCartPrimaryButton(
    text = "Add Item",
    onClick = { /* action */ }
)
```

### Card with Content
```kotlin
SuperCartElevatedCard {
    Text(
        text = "Card Content",
        style = MaterialTheme.typography.titleMedium
    )
}
```

### Input Field
```kotlin
SuperCartOutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = "Item Name"
)
```

## Best Practices

1. **Consistency**: Always use the design system components instead of creating custom ones
2. **Spacing**: Use the predefined spacing values for consistent layouts
3. **Colors**: Use semantic colors (primary, secondary) rather than hardcoded values
4. **Typography**: Use the typography scale for consistent text hierarchy
5. **Shapes**: Use predefined shapes for consistent visual language

## Migration from Old Project

This design system maintains visual consistency with the original SuperCart project while providing:
- Better organization and reusability
- Consistent spacing and sizing
- Material Design 3 compliance
- Easy customization and theming
- Comprehensive documentation
