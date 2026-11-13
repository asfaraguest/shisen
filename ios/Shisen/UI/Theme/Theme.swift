import SwiftUI

public struct ShisenTheme {
    // Deep Glacial Obsidian Color Palette
    public static let background = Color(red: 10/255, green: 12/255, blue: 16/255)
    public static let surface = Color(red: 18/255, green: 22/255, blue: 28/255)
    public static let surfaceElevated = Color(red: 26/255, green: 32/255, blue: 42/255)
    
    // Accents
    public static let accent = Color(red: 100/255, green: 149/255, blue: 237/255) // Cornflower Blue
    public static let accentCyan = Color(red: 72/255, green: 209/255, blue: 204/255)
    public static let accentRed = Color(red: 255/255, green: 75/255, blue: 95/255)
    
    // Text colors
    public static let textPrimary = Color.white
    public static let textSecondary = Color(white: 0.7)
    public static let textMuted = Color(white: 0.45)
}

public struct GlassModifier: ViewModifier {
    public func body(content: Content) -> some View {
        content
            .background(.ultraThinMaterial)
            .background(ShisenTheme.surface.opacity(0.7))
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
            )
    }
}

public extension View {
    func glassCard() -> some View {
        self.modifier(GlassModifier())
    }
}
