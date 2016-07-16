package sa.edu.kaust.exceptions;

public class PhenotypeFormatException extends Exception {

    public PhenotypeFormatException () {
    }

    public PhenotypeFormatException(String message) {
        super(message);
    }

    public PhenotypeFormatException(Throwable cause) {
        super(cause);
    }

    public PhenotypeFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
