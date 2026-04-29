package com.example.laba4.pointChecker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitCheckServiceTest {

    private final HitCheckService hitCheckService = new HitCheckService();

    @Test
    void checkTrue() {
        assertTrue(hitCheckService.checkHit(1.0, 1.0, 2));
    }

    @Test
    void checkFalse() {
        assertFalse(hitCheckService.checkHit(3.0, 3.0, 2));
    }

    @Test
    void checkBorder() {
        assertTrue(hitCheckService.checkHit(2.0, 0.0, 2));
    }

    @Test
    void checkFirst(){
        assertTrue(hitCheckService.checkHit(0.5, 0.5, 2));
    }    

    @Test
    void checkSecond(){
        assertFalse(hitCheckService.checkHit(-0.5, 0.5, 2));
    }

    @Test
    void checkThird(){
        assertTrue(hitCheckService.checkHit(-0.5, -0.5, 2));
    }

    @Test
    void checkFourth(){
        assertTrue(hitCheckService.checkHit(0.5, -0.5, 2));
    }
}
