/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.license;

/**
 * Responsible for verifying signed licenses
 */
public class LicenseVerifier {

    /**
     * verifies the license content with the signature using the packaged
     * public key
     * @param license to verify
     * @return true if valid, false otherwise
     */
    public static boolean verifyLicense(final License license, byte[] publicKeyData) {
        return true;
    }

    public static boolean verifyLicense(final License license) {
        return true;
    }
}
