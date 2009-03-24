package joist.web.util;

import java.io.IOException;
import java.io.Writer;

import joist.util.StringBuilderr;
import joist.web.Control;
import joist.web.exceptions.IoException;

import org.apache.commons.lang.ObjectUtils;

/** Add HTML-specific helper methods to the {@link StringBuilderr} class.
 *
 * Besides <code>start</code>, <code>attribute</code>, etc., we override
 * <code>interpolate</code> to recognize replacements like:
 *
 *     sb.line("<tag attribute={}></tag>", value);
 *
 * And we'll auto-wrap value in double quotes because the immediately-
 * proceeding character to the <code>{}</code> replacement is <code>=</code>.
 */
public class HtmlWriter extends Writer {

    private final Writer w;

    public HtmlWriter(Writer w) {
        this.w = w;
    }

    public void line(String line, Object... args) {
        this.append(line, args);
        this.append("\n");
    }

    public void append(String pattern) {
        this.append(pattern, new Object[] {});
    }

    public void append(String pattern, Object... args) {
        int arg = 0;
        int at = 0;
        int br;
        while ((br = pattern.indexOf("{}", at)) != -1) {
            this.write(pattern.substring(at, br));
            if (arg < args.length) {
                boolean wrapInQuotes = br > 0 && pattern.charAt(br - 1) == '=';
                if (wrapInQuotes) {
                    this.write("\"");
                }
                Object value = args[arg++];
                if (value instanceof Control) {
                    ((Control) value).render(this);
                } else {
                    this.write(ObjectUtils.toString(value));
                }
                if (wrapInQuotes) {
                    this.write("\"");
                }
            }
            at = br + 2;
        }
        if (at != pattern.length()) {
            this.write(pattern.substring(at));
        }
    }

    public void start(String element) {
        this.append("<");
        this.append(element);
    }

    public void attribute(String name, String value) {
        this.append(" ");
        this.append(name);
        this.append("=\"");
        this.append(value);
        this.append("\"");
    }

    public void startDone() {
        this.append(">");
    }

    public void end(String element) {
        this.append("</");
        this.append(element);
        this.append(">");
    }

    public void close() {
        try {
            this.w.close();
        } catch (IOException io) {
            throw new IoException(io);
        }
    }

    public void flush() {
        try {
            this.w.flush();
        } catch (IOException io) {
            throw new IoException(io);
        }
    }

    public void write(char[] c, int i, int j) {
        try {
            this.w.write(c, i, j);
        } catch (IOException io) {
            throw new IoException(io);
        }
    }

    public void write(String str) {
        try {
            this.w.write(str);
        } catch (IOException io) {
            throw new IoException(io);
        }
    }

}