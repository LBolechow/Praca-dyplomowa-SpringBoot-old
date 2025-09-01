package pl.lukbol.dyplom.utilities;

import java.util.Random;

public class GenerateCode {
    public static String generateActivationCode() {
        int codeLength = 12;
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < codeLength; i++) {
            int index = new Random().nextInt(characters.length());
            code.append(characters.charAt(index));
        }

        return code.toString();
    }
}
