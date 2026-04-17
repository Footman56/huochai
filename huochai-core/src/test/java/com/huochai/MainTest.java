package com.huochai;

import java.util.regex.Pattern;

import lombok.Data;

/**
 *
 *@author peilizhi 
 *@date 2026/4/17 11:15
 **/
@Data
public class MainTest {



    /**
     * 7级、8级、9级、10级、11级、12级
     * T10、P10、E10、T11、P11、E11、T12、P12、E12
     * M3A、M3B、M3C、M4A、M4B、M5A、M5B
     */
    private static final String SALARYCERTIFICATE_LEVEL_REGEX = "^((7|8|9|1[0-2])级|[TPE]1[0-2]|M(3[A-C]|4[A-B]|5[A-B]))$";

    public static void main(String[] args) {
        String[] tests = {
                // ✅ 合法
                "7级", "8级", "9级", "10级", "11级", "12级",
                "T10", "P10", "E10", "T11", "P11", "E11", "T12", "P12", "E12",
                "M3A", "M3B", "M3C", "M4A", "M4B", "M5A", "M5B",

                // ❌ 非法
                "6级", "13级",
                "T9", "P13",
                "M2A", "M6A",
                "M3D", "M4C"
        };

        Pattern LEVEL_PATTERN = Pattern.compile(SALARYCERTIFICATE_LEVEL_REGEX);

        for (String test : tests) {
            System.out.printf("%-5s -> %s%n", test, LEVEL_PATTERN.matcher(test).matches());
        }
    }
}
