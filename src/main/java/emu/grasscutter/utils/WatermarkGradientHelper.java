package emu.grasscutter.utils;

import emu.grasscutter.config.ConfigContainer;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.server.packet.send.UidWatermarkScript;

/** Builds configured watermark text and applies a compact color gradient when it fits. */
public final class WatermarkGradientHelper {
    private static final int MID_BLUE = 0x0EA5E9;

    private WatermarkGradientHelper() {}

    public static String buildDisplayText(
            ConfigContainer.GameOptions.WatermarkOptions options, Player player) {
        String text = options.text == null ? "" : options.text.trim();
        if (player == null || player.getUid() <= 0) return text;

        String uid = Integer.toString(player.getUid());
        if (text.contains("{uid}")) return text.replace("{uid}", uid);
        if (text.endsWith("|") || text.endsWith("｜")) return text + uid;
        if (text.isEmpty()) return uid;
        return text + "｜" + uid;
    }

    public static String applyGradient(
            ConfigContainer.GameOptions.WatermarkOptions options, String text) {
        int start = RichTextUtils.parseColor(options.color);
        int end = RichTextUtils.parseColor(options.gradientTo);
        if (start < 0 && end < 0) return text;
        if (start < 0) start = end;
        if (end < 0) end = start;

        String gradient = triGradient(text, start, MID_BLUE, end);
        if (fits(gradient)) return gradient;

        String compact = compactGradient(text, start, MID_BLUE, end);
        return fits(compact) ? compact : text;
    }

    private static boolean fits(String text) {
        return UidWatermarkScript.forText(text) != null;
    }

    private static int colorAt(float position, int start, int middle, int end) {
        return position <= 0.5f
                ? RichTextUtils.lerpColor(start, middle, position * 2f)
                : RichTextUtils.lerpColor(middle, end, (position - 0.5f) * 2f);
    }

    private static String triGradient(String text, int start, int middle, int end) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                result.append(current);
            } else {
                float position = length <= 1 ? 0f : (float) i / (length - 1);
                result.append(
                        RichTextUtils.colorize(
                                String.valueOf(current), colorAt(position, start, middle, end)));
            }
        }
        return result.toString();
    }

    private static String compactGradient(String text, int start, int middle, int end) {
        if (text == null || text.isEmpty()) return text;
        int length = text.length();
        for (int segments = length; segments >= 1; segments--) {
            StringBuilder result = new StringBuilder();
            for (int segment = 0; segment < segments; segment++) {
                int from = segment * length / segments;
                int to = (segment + 1) * length / segments;
                float position = length <= 1 ? 0f : ((from + to - 1) / 2f) / (length - 1);
                result.append(RichTextUtils.colorize(
                        text.substring(from, to), colorAt(position, start, middle, end)));
            }
            String candidate = result.toString();
            if (fits(candidate)) return candidate;
        }
        return text;
    }
}
