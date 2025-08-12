package pl.lukbol.dyplom.utilities;

import java.util.Random;

public class GenerateCode {
    private static String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String generateActivationCode() {
        int codeLength = 12;
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < codeLength; i++) {
            int index = new Random().nextInt(CHARACTERS.length());
            code.append(CHARACTERS.charAt(index));
        }

        return code.toString();
    }
}
