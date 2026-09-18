package org.mnm.cli;

import java.util.Scanner;

public class LineReader {

    String readLine() {
        Scanner s = new Scanner(System.in);
        String line = s.nextLine().trim();
        s.close();
        return line;
    }
}
