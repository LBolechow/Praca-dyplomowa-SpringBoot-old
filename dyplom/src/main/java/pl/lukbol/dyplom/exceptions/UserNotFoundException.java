package pl.lukbol.dyplom.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Brak użytkownika o id:  " + id);
    }
}
