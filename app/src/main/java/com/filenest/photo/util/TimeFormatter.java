package com.filenest.photo.util;

import java.util.Locale;

public class TimeFormatter {

    /**
     * 将毫秒转换为 "时:分:秒" 格式
     * 示例：
     * - 3825000 毫秒 → 1:03:45（1小时3分45秒）
     * - 1425000 毫秒 → 23:45（23分45秒）
     * - 45000 毫秒 → 00:45（45秒）
     */
    public static String formatTimeToHMS(long millis) {
        // 确保毫秒数不为负数（避免异常）
        long totalSeconds = millis < 0 ? 0 : millis / 1000;

        // 计算小时、分钟、秒
        long hours = totalSeconds / 3600;
        long remainingSecondsAfterHours = totalSeconds % 3600;
        long minutes = remainingSecondsAfterHours / 60;
        long seconds = remainingSecondsAfterHours % 60;

        // 根据是否有小时部分，返回不同格式
        if (hours > 0) {
            // 有小时：时:分:秒（小时不补零，分钟和秒补零）
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            // 无小时：分:秒（分钟和秒补零）
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

}
