package de.uol.pgdoener.civicsage.source;

import de.uol.pgdoener.civicsage.index.exception.ReadFileException;
import de.uol.pgdoener.civicsage.source.exception.HashingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
public class FileHashingService {

    private static final String ALGORITHM = "SHA-256";

    public String hash(MultipartFile file) {
        try {
            return hashInternal(file);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error while hashing file: ", e);
            throw new HashingException("Hashing algorithm exception", e);
        } catch (IOException e) {
            log.warn("Could not read file while hashing", e);
            throw new ReadFileException("Could not read file while hashing", e);
        }
    }

    private String hashInternal(MultipartFile file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        try (var digestIn = new DigestInputStream(file.getInputStream(), digest)) {
            digestIn.readAllBytes();
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        log.debug("Hash for file is {}", hash);
        return hash;
    }

}
