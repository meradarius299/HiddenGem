package com.hiddengems.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoUtilsTest {

    @Test
    void samePointIsZeroMetersApart() {
        assertEquals(0, GeoUtils.distanceMeters(45, 25, 45, 25), 0.001);
    }

    @Test
    void oneThousandthOfADegreeOfLatitudeIsAbout111Meters() {
        assertEquals(111.2, GeoUtils.distanceMeters(45, 25, 45.001, 25), 0.5);
    }

    @Test
    void oneDegreeOfLongitudeAtTheEquatorIsAbout111Kilometers() {
        assertEquals(111_195, GeoUtils.distanceMeters(0, 0, 0, 1), 10);
    }
}