package org.beynet.controller;

import java.util.Random;

/**
 * Created by beynet on 18/10/2014.
 */
public class PasswordGenerator {
    static final char[] LETTERS=new char[26*2];
    static final char[] NUMBERS=new char[10];
    static final char[] SYMBOLS={'@','&','_','!','§','%'};
    static Random r = new Random(System.currentTimeMillis());
    static {
        // init numbers
        for (int i=0;i<10;i++) {
            NUMBERS[i]=(char)('0'+i);
        }
        // init letters
        int offset=0;
        for (int i=0;i<26;i++) {
            LETTERS[offset+i]=(char)('A'+i);
        }
        offset+=26;
        for (int i=0;i<26;i++) {
            LETTERS[offset+i]=(char)('a'+i);
        }
        r.nextInt(1000);
    }

    /**
     *
     * @param length password expected length
     * @param digits numbers of digits to be contained in the password
     * @param symbols numbers of symbols to be contained in the password
     * @return
     */
    public static String generateNewPassword(int length,int digits,int symbols) {
        StringBuilder result = new StringBuilder();
        if (digits<0) throw new IllegalArgumentException("numbers must be >0");
        if (symbols<0) throw new IllegalArgumentException("symbols must be >0");
        int letters = length-digits-symbols;
        if ((digits+symbols)>length) throw new IllegalArgumentException("symbols+numbers must be <=length");
        StringBuilder resultOrdered = new StringBuilder();
        for (int i=0;i<digits;i++) {
            int n = r.nextInt(NUMBERS.length);
            resultOrdered.append(NUMBERS[n]);
        }
        for (int i=0;i<symbols;i++) {
            int n = r.nextInt(SYMBOLS.length);
            resultOrdered.append(SYMBOLS[n]);
        }
        for (int i=0;i<letters;i++) {
            int n = r.nextInt(LETTERS.length);
            resultOrdered.append(LETTERS[n]);
        }

        //generate a result not ordered from resultOrdered
        for (int i=0;i<length;i++) {
            int next = r.nextInt(resultOrdered.length());
            result.append(resultOrdered.charAt(next));
            resultOrdered.delete(next,next+1);
        }
        return result.toString();
    }
}
