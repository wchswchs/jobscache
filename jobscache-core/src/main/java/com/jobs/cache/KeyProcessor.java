package com.jobs.cache;

public class KeyProcessor {

    public static String extractDomain(Object key) {
        String domain = "";
        String keyStr = String.valueOf(key);

        String[] keyArr = keyStr.split(":");
        domain += keyStr.replace(":" + keyArr[keyArr.length - 1], "");
        String[] versionDomainArr = domain.split("_");
        domain = domain.replace("_" + versionDomainArr[versionDomainArr.length - 1], "");

        return domain;
    }

    public static String extractDomain2(Object domain) {
        String keyStr = String.valueOf(domain);

        String[] keyArr = keyStr.split(":");
        String domainStr = keyStr.replace(keyArr[0] + ":", "");
        String[] versionDomainArr = domainStr.split("_");
        return domainStr.replace("_" + versionDomainArr[versionDomainArr.length - 1], "");
    }

    public static String extractAbsolutePathDomain(Object key) {
        String domain = "";
        String keyStr = String.valueOf(key);

        String[] keyArr = keyStr.split(":");
        domain += keyStr.replace(":" + keyArr[keyArr.length - 1], "");
        domain = domain.replace(keyArr[0] + ":", "");
        String[] versionDomainArr = domain.split("_");
        domain = domain.replace("_" + versionDomainArr[versionDomainArr.length - 1], "");

        return domain;
    }

    public static String extractKey(Object key) {
        String keyStr = String.valueOf(key);

        String[] keyArr = keyStr.split(":");
        return keyArr[keyArr.length - 1];
    }

    public static String trimKey(Object key) {
        String keyStr = String.valueOf(key);

        String[] keyArr = keyStr.split(":");
        return keyStr.replace(":" + keyArr[keyArr.length - 1], "");
    }

    public static String convertPattern(Object domain) {
        return domain + ":*";
    }

}
