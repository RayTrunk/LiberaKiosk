package com.axon.kiosk

import android.content.Context
import android.util.Log
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

/**
 * Manages SSL/TLS certificates for Libera Kiosk web administration.
 * Generates self-signed certificates branded to Libera Kiosk Solutions.
 */
class CertificateManager(private val context: Context) {

    companion object {
        private const val TAG = "CertificateManager"
        private const val KEYSTORE_FILE = "libera_kiosk.bks"
        private const val KEYSTORE_PASSWORD = "liberakiosk2026"
        private const val KEY_ALIAS = "liberakiosk"
        private const val CERTIFICATE_VALIDITY_YEARS = 10
        
        // Libera Kiosk Solutions branding
        private const val CERT_DN = "CN=Libera Kiosk Admin, OU=Kiosk Solutions, O=Libera Kiosk Solutions, L=Berlin, ST=Berlin, C=DE"
        
        init {
            // Register BouncyCastle provider
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }

    private val keystoreFile: File
        get() = File(context.filesDir, KEYSTORE_FILE)

    /**
     * Check if a certificate exists
     */
    fun hasCertificate(): Boolean {
        return keystoreFile.exists()
    }

    /**
     * Get certificate info for display
     */
    fun getCertificateInfo(): String {
        if (!hasCertificate()) return "No certificate"
        
        return try {
            val keyStore = loadKeyStore()
            val cert = keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
            if (cert != null) {
                val subject = cert.subjectX500Principal.name
                val validFrom = cert.notBefore
                val validTo = cert.notAfter
                val serial = cert.serialNumber.toString(16).uppercase()
                
                "Issuer: Libera Kiosk Solutions\n" +
                "Subject: $subject\n" +
                "Valid: ${formatDate(validFrom)} - ${formatDate(validTo)}\n" +
                "Serial: $serial"
            } else {
                "Certificate not found"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading certificate", e)
            "Error: ${e.message}"
        }
    }

    /**
     * Generate a new self-signed certificate using BouncyCastle
     */
    fun generateCertificate(): Boolean {
        return try {
            Log.i(TAG, "Generating new self-signed certificate for Libera Kiosk Solutions...")
            
            // Generate RSA key pair
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
            keyPairGenerator.initialize(2048, SecureRandom())
            val keyPair = keyPairGenerator.generateKeyPair()
            
            // Calculate validity dates
            val now = Date()
            val calendar = Calendar.getInstance()
            calendar.time = now
            calendar.add(Calendar.YEAR, CERTIFICATE_VALIDITY_YEARS)
            val expiry = calendar.time
            
            // Create X500Name for subject and issuer (self-signed)
            val issuer = X500Name(CERT_DN)
            val serial = BigInteger(64, SecureRandom())
            
            // Build certificate
            val certBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
                issuer,
                serial,
                now,
                expiry,
                issuer,
                keyPair.public
            )
            
            // Add extensions
            certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                BasicConstraints(true)
            )
            
            certBuilder.addExtension(
                Extension.keyUsage,
                true,
                KeyUsage(KeyUsage.keyCertSign or KeyUsage.digitalSignature or KeyUsage.keyEncipherment)
            )
            
            // Sign the certificate
            val signer = JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.private)
            
            val certHolder = certBuilder.build(signer)
            val cert = JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder)
            
            // Store in keystore
            val keyStore = KeyStore.getInstance("BKS", "BC")
            keyStore.load(null, KEYSTORE_PASSWORD.toCharArray())
            keyStore.setKeyEntry(
                KEY_ALIAS,
                keyPair.private,
                KEYSTORE_PASSWORD.toCharArray(),
                arrayOf<Certificate>(cert)
            )
            
            // Save keystore
            FileOutputStream(keystoreFile).use { fos ->
                keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray())
            }
            
            Log.i(TAG, "Certificate generated successfully for Libera Kiosk Solutions")
            Log.i(TAG, "Subject: $CERT_DN")
            Log.i(TAG, "Valid until: $expiry")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate certificate", e)
            false
        }
    }

    /**
     * Delete existing certificate
     */
    fun deleteCertificate(): Boolean {
        return try {
            if (keystoreFile.exists()) {
                keystoreFile.delete()
                Log.i(TAG, "Certificate deleted")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete certificate", e)
            false
        }
    }

    /**
     * Get SSLServerSocketFactory for HTTPS server
     */
    fun getSSLServerSocketFactory(): SSLServerSocketFactory? {
        return try {
            if (!hasCertificate()) {
                Log.w(TAG, "No certificate found, generating new one...")
                if (!generateCertificate()) {
                    return null
                }
            }
            
            val keyStore = loadKeyStore()
            
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray())
            
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, SecureRandom())
            
            sslContext.serverSocketFactory
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SSL factory", e)
            null
        }
    }

    /**
     * Load keystore from file
     */
    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("BKS", "BC")
        if (keystoreFile.exists()) {
            FileInputStream(keystoreFile).use { fis ->
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray())
            }
        } else {
            keyStore.load(null, KEYSTORE_PASSWORD.toCharArray())
        }
        return keyStore
    }

    private fun formatDate(date: Date): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(date)
    }
}
