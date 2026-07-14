package com.example.syntheticfit.routing;

import com.example.syntheticfit.routing.domain.GeoUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class GeoUtilsTest {

    @Test
    void distanceBetweenSamePoints_isZero() {
        assertThat(GeoUtils.distanceMeters(51.0, 17.0, 51.0, 17.0)).isEqualTo(0.0);
    }

    @Test
    void distanceKnownPair() {
        // Warsaw to Krakow approx 250km
        double d = GeoUtils.distanceMeters(52.2297, 21.0122, 50.0647, 19.9450);
        assertThat(d).isCloseTo(252_000, within(5_000.0));
    }

    @Test
    void decodePolyline_returnsCoordinates() {
        // encode of single segment: 38.5,-120.2 to 40.7,-120.95 to 43.252,-126.453
        String encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        double[][] points = GeoUtils.decodePolyline(encoded);
        assertThat(points.length).isEqualTo(3);
        assertThat(points[0][0]).isCloseTo(38.5, within(0.0001));
        assertThat(points[0][1]).isCloseTo(-120.2, within(0.0001));
    }
}
