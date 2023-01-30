package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.zxing.common.StringUtils;
import com.lqr.emoji.EmojiManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.wildfire.chat.kit.R;

public class EmojiDisableFilter implements InputFilter {

    private Context context;
    private Pattern emoji;
    private Toast toast;

    //
    public EmojiDisableFilter(Context context) {
        this.context = context;
        String pattern = "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]";
        String pattern2 = "[0-9a-zA-Z|\u4e00-\u9fa5]";
        emoji = Pattern.compile(pattern2,
                Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (TextUtils.isEmpty(source) || " ".contentEquals(source)) {
            return null;
        }
        Matcher emojiMatcher = emoji.matcher(source);
        if (!emojiMatcher.find()) {
            if (toast != null) {
                toast.cancel();
            }
            toast = Toast.makeText(context, context.getText(R.string.toast_name_emoji_error), Toast.LENGTH_SHORT);
            toast.show();
            return "";
        }
        return null;
    }

    public String filterEmoji(String source) {
        if (TextUtils.isEmpty(source)) {
            return null;
        }
        StringBuilder buf = null;
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);
            if (isEmojiCharacter(codePoint)) {
                if (buf == null) {
                    buf = new StringBuilder(source.length());
                }
                buf.append(codePoint);
            }
        }
        if (buf == null) {
            return source;
        } else {
            if (buf.length() == len) {
                buf = null;
                return source;
            } else {
                return buf.toString();
            }
        }
    }

    private boolean isEmojiCharacter(char codePoint) {
        return (codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA)
                || (codePoint == 0xD)
                || ((codePoint >= 0x20) && (codePoint <= 0xD7FF))
                || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
                || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF));
    }
}