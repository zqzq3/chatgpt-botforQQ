package com.eyu.config;

import net.mamoe.mirai.utils.BotConfiguration;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class FixProtocolVersion {
    public static void fix(){
        try {
            Class<?> MiraiProtocolInternal = Class.forName("net.mamoe.mirai.internal.utils.MiraiProtocolInternal");
            Field field = MiraiProtocolInternal.getFields()[0];
            Object companion = field.get(Object.class);

            EnumMap<BotConfiguration.MiraiProtocol, Object> protocols = (EnumMap<BotConfiguration.MiraiProtocol, Object>)companion.getClass().getMethod("getProtocols$mirai_core").invoke(companion);
            Object mac = protocols.get(BotConfiguration.MiraiProtocol.MACOS);
        /*
        * apkId: String,
            id: Long,
            ver: String,
            sdkVer: String,
            miscBitMap: Int,
            subSigMap: Int,
            mainSigMap: Int,
            sign: String,
            buildTime: Long,
            ssoVersion: Int,
            canDoQRCodeLogin: Boolean = false,
        * */
            Class<?> macClass = mac.getClass();
            Map<String, Object> macData = new HashMap<String, Object>(){{
                put("id", 537128930);
                put("ver", "5.8.9");
                put("sdkVer", "6.0.0.2433");
                put("buildTime", 1595836208L);
                put("sign", "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7");
                put("ssoVersion",12);
                put("miscBitMap", 150470524);
                put("subSigMap", 66560);
                put("mainSigMap", 1970400);
            }};
            for (Field f : macClass.getFields()) {
                f.setAccessible(true);
                if(macData.containsKey(f.getName())){
                    f.set(mac, macData.get(f.getName()));
                }
                f.setAccessible(false);
            }

            Object pad = protocols.get(BotConfiguration.MiraiProtocol.IPAD);
            Class<?> padClass = mac.getClass();
            Map<String, Object> padData = new HashMap<String, Object>(){{
                put("id", 537151363);
                put("ver", "8.9.33.614");
                put("sdkVer", "6.0.0.2433");
                put("buildTime", 1640921786L);
                put("sign", "AA 39 78 F4 1F D9 6F F9 91 4A 66 9E 18 64 74 C7");
                put("ssoVersion",12);
                put("miscBitMap", 150470524);
                put("subSigMap", 66560);
                put("mainSigMap", 1970400);
            }};
            for (Field f : padClass.getFields()) {
                f.setAccessible(true);
                if(padData.containsKey(f.getName())){
                    f.set(pad, padData.get(f.getName()));
                }
                f.setAccessible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}