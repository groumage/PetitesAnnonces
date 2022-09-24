package main.protocol;

public enum ErrorLogMessage {
    NAME_NOT_VALID("Name not valid"),
    MAIL_ALREADY_TAKEN("Mail already taken"),
    MAIL_NOT_VALID("Mail not valid"),
    COMBINATION_MAIL_PWD_INVALID("Combination mail pwd invalid"),
    NOT_RESPONDING_TO_REQUEST("Not responding to request"),
    NO_ANNONCE_IN_THAT_DOMAIN("There is no annonce in that domain"),
    NOT_OWNER("Not owner of that annonce");

    private String content;

    ErrorLogMessage(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
