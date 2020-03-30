package pt.tecnico.sauron.A20.exceptions;

public enum ErrorMessage {
    UNKNOWN("An unknown exception occurred."),
    DUPLICATE_CAMERA("Camera already exists."),
    DUPLICATE_CAM_NAME("Camera name already in use."),
    INVALID_COORDINATES("Invalid coordinates provided."),
    INVALID_CAM_NAME("Invalid camera name provided."),
    INVALID_CAR_ID("Invalid car identifier provided."),
    INVALID_PERSON_IDENTIFIER("Invalid person identifier provided."),
    CAMERA_NOT_FOUND("Camera not found."),
    TYPE_DOES_NOT_EXIST("The type does not exist."),
    INVALID_ID("Invalid identifier provided."),
    OBJECT_NOT_FOUND("The requested Sauron Object was not found."),
    ERROR_PROCESSING_FILE("Error processing provided file."),
    INVALID_ARGUMENT("Invalid argument for requested method.");

    public final String label;

    ErrorMessage(String label) {
        this.label = label;
    }
}
