package org.beynet.controller;

import java.util.Random;

/**
 * Created by beynet on 18/10/2014.
 */
public class PasswordGenerator {
    static final char[] LETTERS=new char[26*2];
    static final char[] NUMBERS=new char[10];
    static final char[] SYMBOLS={'@','&','_','!','§','%'};
    static Random r = new Random();
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
    }

    public static String generateNewPassword(int length,int numbers,int symbols) {
        StringBuilder result = new StringBuilder();
        if (numbers<0) throw new IllegalArgumentException("numbers must be >0");
        if (symbols<0) throw new IllegalArgumentException("symbols must be >0");
        int letters = length-numbers-symbols;
        if ((numbers+symbols)>length) throw new IllegalArgumentException("symbols+numbers must be <=length");
        StringBuilder resultLetters = new StringBuilder();
        StringBuilder resultNumbers = new StringBuilder();
        StringBuilder resultSymbols = new StringBuilder();
        for (int i=0;i<numbers;i++) {
            int n = r.nextInt(NUMBERS.length);
            resultNumbers.append(NUMBERS[n]);
        }
        for (int i=0;i<symbols;i++) {
            int n = r.nextInt(SYMBOLS.length);
            resultSymbols.append(SYMBOLS[n]);
        }
        for (int i=0;i<letters;i++) {
            int n = r.nextInt(LETTERS.length);
            resultLetters.append(LETTERS[n]);
        }

        for (int i=0;i<length;i++) {
            int typesMax = 0;
            if (numbers>0) typesMax++;
            if (symbols>0) typesMax++;
            if (letters>0) typesMax++;
            int type = (typesMax!=1)?r.nextInt(typesMax):0;
            if (type==0) {
                if (letters>0) {
                    result.append(resultLetters.charAt(-1+letters--));
                } else if (numbers>0) {
                    result.append(resultNumbers.charAt(-1+numbers--));
                }
                else {
                    result.append(resultSymbols.charAt(-1+symbols--));
                }
            }
            else if (type==1 && numbers>0){
                result.append(resultNumbers.charAt(-1+numbers--));
            }
            else  {
                result.append(resultSymbols.charAt(-1+symbols--));
            }
        }
        return result.toString();
    }
}
