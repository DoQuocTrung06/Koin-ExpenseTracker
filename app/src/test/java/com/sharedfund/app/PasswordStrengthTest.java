package com.sharedfund.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sharedfund.app.util.PasswordStrengthEvaluator;

import org.junit.Test;

public class PasswordStrengthTest {

    @Test
    public void testEmptyAndNullPassword() {
        PasswordStrengthEvaluator.Evaluation emptyEval = PasswordStrengthEvaluator.evaluate("");
        assertNotNull(emptyEval);
        assertEquals(PasswordStrengthEvaluator.Level.EMPTY, emptyEval.getLevel());
        assertEquals(0, emptyEval.getProgressPercent());
        assertEquals("Chưa nhập", emptyEval.getLabel());
        assertFalse(emptyEval.hasMinLength());
        assertFalse(emptyEval.hasUpperAndLower());
        assertFalse(emptyEval.hasDigit());
        assertFalse(emptyEval.hasSpecial());

        PasswordStrengthEvaluator.Evaluation nullEval = PasswordStrengthEvaluator.evaluate(null);
        assertNotNull(nullEval);
        assertEquals(PasswordStrengthEvaluator.Level.EMPTY, nullEval.getLevel());
        assertEquals(0, nullEval.getProgressPercent());
    }

    @Test
    public void testShortPasswordsAreWeak() {
        PasswordStrengthEvaluator.Evaluation shortEval = PasswordStrengthEvaluator.evaluate("12345");
        assertEquals(PasswordStrengthEvaluator.Level.WEAK, shortEval.getLevel());
        assertEquals(33, shortEval.getProgressPercent());
        assertEquals("Mật khẩu yếu", shortEval.getLabel());
        assertFalse(shortEval.hasMinLength());

        PasswordStrengthEvaluator.Evaluation shortMixed = PasswordStrengthEvaluator.evaluate("Aa1!xyz");
        assertEquals(PasswordStrengthEvaluator.Level.WEAK, shortMixed.getLevel());
        assertEquals(33, shortMixed.getProgressPercent());
        assertEquals("Mật khẩu yếu", shortMixed.getLabel());
        assertFalse(shortMixed.hasMinLength());
    }

    @Test
    public void testWeakPasswords() {
        PasswordStrengthEvaluator.Evaluation evalLetters = PasswordStrengthEvaluator.evaluate("abcdefg");
        assertEquals(PasswordStrengthEvaluator.Level.WEAK, evalLetters.getLevel());
        assertEquals(33, evalLetters.getProgressPercent());
        assertFalse(evalLetters.hasMinLength());
        assertFalse(evalLetters.hasDigit());

        PasswordStrengthEvaluator.Evaluation evalDigitsOnly = PasswordStrengthEvaluator.evaluate("12345678");
        assertEquals(PasswordStrengthEvaluator.Level.WEAK, evalDigitsOnly.getLevel());
        assertEquals(33, evalDigitsOnly.getProgressPercent());
        assertTrue(evalDigitsOnly.hasMinLength());
        assertTrue(evalDigitsOnly.hasDigit());
        assertFalse(evalDigitsOnly.hasUpperAndLower());

        PasswordStrengthEvaluator.Evaluation evalLettersOnly = PasswordStrengthEvaluator.evaluate("PasswordLong");
        assertEquals(PasswordStrengthEvaluator.Level.WEAK, evalLettersOnly.getLevel());
        assertEquals(33, evalLettersOnly.getProgressPercent());
        assertTrue(evalLettersOnly.hasMinLength());
        assertTrue(evalLettersOnly.hasUpperAndLower());
        assertFalse(evalLettersOnly.hasDigit());
    }

    @Test
    public void testMediumPasswords() {
        PasswordStrengthEvaluator.Evaluation eval1 = PasswordStrengthEvaluator.evaluate("password123");
        assertEquals(PasswordStrengthEvaluator.Level.MEDIUM, eval1.getLevel());
        assertEquals(66, eval1.getProgressPercent());
        assertEquals("Mật khẩu trung bình", eval1.getLabel());
        assertTrue(eval1.hasMinLength());
        assertTrue(eval1.hasDigit());
        assertFalse(eval1.hasUpperAndLower());
        assertFalse(eval1.hasSpecial());

        PasswordStrengthEvaluator.Evaluation eval2 = PasswordStrengthEvaluator.evaluate("Password123");
        assertEquals(PasswordStrengthEvaluator.Level.MEDIUM, eval2.getLevel());
        assertEquals(66, eval2.getProgressPercent());
        assertEquals("Mật khẩu trung bình", eval2.getLabel());
        assertTrue(eval2.hasMinLength());
        assertTrue(eval2.hasUpperAndLower());
        assertTrue(eval2.hasDigit());
        assertFalse(eval2.hasSpecial());
    }

    @Test
    public void testStrongPasswords() {
        PasswordStrengthEvaluator.Evaluation eval1 = PasswordStrengthEvaluator.evaluate("Password123!");
        assertEquals(PasswordStrengthEvaluator.Level.STRONG, eval1.getLevel());
        assertEquals(100, eval1.getProgressPercent());
        assertEquals("Mật khẩu mạnh", eval1.getLabel());
        assertTrue(eval1.hasMinLength());
        assertTrue(eval1.hasUpperAndLower());
        assertTrue(eval1.hasDigit());
        assertTrue(eval1.hasSpecial());

        PasswordStrengthEvaluator.Evaluation eval2 = PasswordStrengthEvaluator.evaluate("Secure@2026!");
        assertEquals(PasswordStrengthEvaluator.Level.STRONG, eval2.getLevel());
        assertEquals(100, eval2.getProgressPercent());
        assertTrue(eval2.hasMinLength());
        assertTrue(eval2.hasUpperAndLower());
        assertTrue(eval2.hasDigit());
        assertTrue(eval2.hasSpecial());
    }

    @Test
    public void testHasLettersAndNumbers() {
        assertFalse(PasswordStrengthEvaluator.hasLettersAndNumbers(null));
        assertFalse(PasswordStrengthEvaluator.hasLettersAndNumbers(""));
        assertFalse(PasswordStrengthEvaluator.hasLettersAndNumbers("123456789"));
        assertFalse(PasswordStrengthEvaluator.hasLettersAndNumbers("abcdefgh"));
        assertFalse(PasswordStrengthEvaluator.hasLettersAndNumbers("!@#$%^&*"));
        assertTrue(PasswordStrengthEvaluator.hasLettersAndNumbers("abc1"));
        assertTrue(PasswordStrengthEvaluator.hasLettersAndNumbers("A9"));
        assertTrue(PasswordStrengthEvaluator.hasLettersAndNumbers("Password01"));
    }

    @Test
    public void testEdgeCasesResilience() {
        PasswordStrengthEvaluator.Evaluation unicodeEval = PasswordStrengthEvaluator.evaluate("MậtKhẩu@2026");
        assertNotNull(unicodeEval);
        assertTrue(unicodeEval.hasMinLength());
        assertTrue(unicodeEval.hasDigit());
        assertTrue(unicodeEval.hasSpecial());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append("a");
        sb.append("A1!");
        PasswordStrengthEvaluator.Evaluation longEval = PasswordStrengthEvaluator.evaluate(sb.toString());
        assertEquals(PasswordStrengthEvaluator.Level.STRONG, longEval.getLevel());
    }
}
