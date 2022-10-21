package org.beynet.controller;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by beynet on 18/10/2014.
 */
public class PasswordGeneratorTest {

    private boolean isNumber(char c) {
        for (int i=0;i<PasswordGenerator.NUMBERS.length;i++) {
            if (c==PasswordGenerator.NUMBERS[i]) {
                return true;
            }
        }
        return false;
    }
    private boolean isSymbol(char c) {
        for (int i=0;i<PasswordGenerator.SYMBOLS.length;i++) {
            if (c==PasswordGenerator.SYMBOLS[i]) {
                return true;
            }
        }
        return false;
    }


    private void check(Integer max,Integer expectedNumber,Integer expectedSymbols) {
        String s = PasswordGenerator.generateNewPassword(max.intValue(), expectedNumber.intValue(), expectedSymbols.intValue());
        System.out.println(s);
        assertThat(Integer.valueOf(s.length()), is(max));
        int n=0,syb=0;
        for (int i=0;i<s.length();i++) {
            char found = s.charAt(i);
            if (isSymbol(found)) {
                syb++;
            }
            else if (isNumber(found)) {
                n++;
            }
        }

        assertThat(Integer.valueOf(n),is(Integer.valueOf(expectedNumber)));
        assertThat(Integer.valueOf(syb),is(Integer.valueOf(expectedSymbols)));
    }

    @Test
    public void chars() {
        Integer max = Integer.valueOf(20);
        Integer expectedNumber = Integer.valueOf(2);
        Integer expectedSymbols = Integer.valueOf(2);
        for (int i=0;i<1000;i++) {
            check(max,expectedNumber, expectedSymbols);
        }
    }

    @Test
    public void chars2() {
        Integer max = Integer.valueOf(12);
        Integer expectedNumber = Integer.valueOf(0);
        Integer expectedSymbols = Integer.valueOf(1);
        check(max,expectedNumber, expectedSymbols);
    }
    @Test
    public void chars3() {
        Integer max = Integer.valueOf(5);
        Integer expectedNumber = Integer.valueOf(1);
        Integer expectedSymbols = Integer.valueOf(0);
        check(max,expectedNumber, expectedSymbols);
    }
    @Test
    public void chars4() {
        Integer max = Integer.valueOf(12);
        Integer expectedNumber = Integer.valueOf(6);
        Integer expectedSymbols = Integer.valueOf(6);
        check(max,expectedNumber, expectedSymbols);
    }

}
