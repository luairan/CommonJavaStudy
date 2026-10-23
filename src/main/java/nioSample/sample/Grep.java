package nioSample.sample;

/*
 * @(#)Grep.java 1.3 01/12/13 Search a list of files for lines that match a given regular-expression pattern.
 * Demonstrates NIO mapped byte buffers, charsets, and regular expressions. Copyright 2001-2002 Oracle and/or its
 * affiliates. All rights reserved. Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met: -Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following disclaimer. -Redistribution in binary form must reproduct
 * the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. Neither the name of Oracle and/or its affiliates. or the names of
 * contributors may be used to endorse or promote products derived from this software without specific prior written
 * permission. This software is provided "AS IS," without a warranty of any kind. ALL EXPRESS OR IMPLIED CONDITIONS,
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES OR LIABILITIES
 * SUFFERED BY LICENSEE AS A RESULT OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF
 * LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGES. You acknowledge that Software is not designed, licensed or intended for use in the design,
 * construction, operation or maintenance of any nuclear facility.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Grep {

    // Charset and decoder for ISO-8859-15
    private static Charset        charset     = Charset.forName("ISO-8859-15");
    private static CharsetDecoder decoder     = charset.newDecoder();

    // Pattern used to parse lines
    private static Pattern        linePattern = Pattern.compile(".*\r?\n");

    // The input pattern that we're looking for
    private static Pattern        pattern;

    // Compile the pattern from the command line
    //
    private static void compile(String pat) {
        try {
            pattern = Pattern.compile(pat);
        } catch (PatternSyntaxException x) {
            System.err.println(x.getMessage());
            System.exit(1);
        }
    }

    // Stream lines so the final unterminated line is searched too, without a 2 GiB map limit.
    private static void grep(File file) throws IOException {
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(file.toPath(),charset)) {
            String line;
            long number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                if (pattern.matcher(line).find()) System.out.println(file + ":" + number + ":" + line);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Grep pattern file...");
            return;
        }
        compile(args[0]);
        for (int i = 1; i < args.length; i++) {
            File f = new File(args[i]);
            try {
                grep(f);
            } catch (IOException x) {
                System.err.println(f + ": " + x);
            }
        }
    }

}
