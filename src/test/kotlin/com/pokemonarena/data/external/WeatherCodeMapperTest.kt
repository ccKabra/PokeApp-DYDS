package com.pokemonarena.data.external

import com.pokemonarena.domain.entity.WeatherCondition
import kotlin.test.Test
import kotlin.test.assertEquals

class WeatherCodeMapperTest {

    @Test
    fun `fromCode_withCode0Below35C_returnsSunny`() {
        assertEquals(WeatherCondition.SUNNY, WeatherCodeMapper.fromCode(0, 25.0))
    }

    @Test
    fun `fromCode_withCode0Above35C_returnsExtremeSun`() {
        assertEquals(WeatherCondition.EXTREME_SUN, WeatherCodeMapper.fromCode(0, 36.0))
    }

    @Test
    fun `fromCode_withCode1to3_returnsClear`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCodeMapper.fromCode(1, 20.0))
        assertEquals(WeatherCondition.CLEAR, WeatherCodeMapper.fromCode(3, 20.0))
    }

    @Test
    fun `fromCode_withCode45to48_returnsFog`() {
        assertEquals(WeatherCondition.FOG, WeatherCodeMapper.fromCode(45, 15.0))
        assertEquals(WeatherCondition.FOG, WeatherCodeMapper.fromCode(48, 15.0))
    }

    @Test
    fun `fromCode_withDrizzleAndRainCodes_returnsRain`() {
        assertEquals(WeatherCondition.RAIN, WeatherCodeMapper.fromCode(51, 18.0))
        assertEquals(WeatherCondition.RAIN, WeatherCodeMapper.fromCode(61, 18.0))
        assertEquals(WeatherCondition.RAIN, WeatherCodeMapper.fromCode(80, 18.0))
    }

    @Test
    fun `fromCode_withCode64to67_returnsStorm`() {
        assertEquals(WeatherCondition.STORM, WeatherCodeMapper.fromCode(65, 18.0))
    }

    @Test
    fun `fromCode_withSnowCodes_returnsSnow`() {
        assertEquals(WeatherCondition.SNOW, WeatherCodeMapper.fromCode(71, -5.0))
        assertEquals(WeatherCondition.SNOW, WeatherCodeMapper.fromCode(85, -2.0))
    }

    @Test
    fun `fromCode_withCode95to99_returnsStorm`() {
        assertEquals(WeatherCondition.STORM, WeatherCodeMapper.fromCode(95, 20.0))
        assertEquals(WeatherCondition.STORM, WeatherCodeMapper.fromCode(99, 22.0))
    }

    @Test
    fun `fromCode_withUnknownCode_returnsClear`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCodeMapper.fromCode(999, 20.0))
    }
}
