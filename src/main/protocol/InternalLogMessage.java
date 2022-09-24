package main.protocol;

public class InternalLogMessage {
    private TokenInternalLogMessage token;
    private Object[] content;

    public InternalLogMessage(TokenInternalLogMessage token) {
        this.token = token;
    }

    public InternalLogMessage(TokenInternalLogMessage token, Object... content) {
        this.token = token;
        this.content = content;
    }

    @Override
    public String toString() {
        switch (this.token) {
            case CLIENT_LOG_SOCKET_OPEN -> {
                return "[INTERNAL] Socket to server is open";
            }
            case CLIENT_LOG_SOCKET_CLOSED -> {
                return "[INTERNAL] Socket to server is closed";
            }
            case CLIENT_LOG_SIGN_UP_OK -> {
                return "[INTERNAL] Sign up ok: (mail=\"" + this.content[0] + "\", name=\"" + this.content[1] + "\")";
            }
            case CLIENT_LOG_SIGN_UP_KO -> {
                return "[INTERNAL] Failed to sign up: (error=\"" + this.content[0] + "\")";
            }
            case SERVER_LOG_CLIENT_HANDLER_CREATED -> {
                return "[INTERNAL] New client handler: (id=" + this.content[0] + ")";
            }
            case CLIENT_LOG_UNREACHABLE_SERVER -> {
                return "[INTERNAL] Unreachable server";
            }
            case CLIENT_LOG_SIGN_IN_OK -> {
                return "[INTERNAL] Sign in ok: (mail=\"" + this.content[0] + "\")";
            }
            case CLIENT_LOG_SIGN_IN_KO -> {
                return "[INTERNAL] Failed to sign in: (error=\"" + this.content[0] + ")";
            }
            case CLIENT_LOG_SIGN_OUT_OK -> {
                return "[INTERNAL] Sign out ok";
            }
            case CLIENT_LOG_SIGN_OUT_KO -> {
                return "[INTERNAL] Sign out ko: (error=\"" + this.content[0] + "\")";
            }
            case SERVER_LOG_CLIENT_CREATED -> {
                return "[INTERNAL] New client: (mail=\"" + this.content[0] + "\", name=\"" + this.content[1] + "\")";
            }
            case SERVER_LOG_CLIENT_HANDLER -> {
                return "[INTERNAL] Close client handler: (id=\"" + this.content[0] + "\")";
            }
            case CLIENT_LOG_ANNONCE_CREATED_OK -> {
                return "[INTERNAL] Annonce created: (title=\"" + this.content[0] + "\")";
            }
            case CLIENT_LOG_ANNONCE_CREATED_KO -> {
                return "[INTERNAL] Annonce not created: (error=\"" + this.content[0] + "\")";
            }
            case CLIENT_LOG_ANNONCE_UPDATED_OK -> {
                return "[INTERNAL] Annonce has been updated";
            }
            case CLIENT_LOG_ANNONCE_UPDATED_KO -> {
                return "[INTERNAL] Annonce not updated: (error=\"" + this.content[0] + "\")";
            }
            case CLIENT_LOG_REMOVE_ANNONCE_OK -> {
                return "[INTERNAL] Annonce has been removed";
            }
            case CLIENT_LOG_REMOVE_ANNONCE_KO -> {
                return "[INTERNAL] Annonce not removed: (error=\"" + this.content[0] + "\")";
            }
            case SERVER_LOG_REMOVE_ANNONCE -> {
                return "[INTERNAL] Annonce removed: (owner=\"" + this.content[0] + "\", title=\"" + this.content[1] + "\", content=\"" + this.content[2] + "\", domain=\"" + this.content[3] + "\", price=" + this.content[4] + ", id=" + this.content[5] + ")";
            }
            case SERVER_LOG_CREATE_ANNONCE -> {
                return "[INTERNAL] Annonce created: (owner=\"" + this.content[0] + "\", title=\"" + this.content[1] + "\", content=\"" + this.content[2] + "\", domain=\"" + this.content[3] + "\", price=" + this.content[4] + ", id=" + this.content[5] + ")";
            }
            case SERVER_LOG_UPDATE_ANNONCE -> {
                return "[INTERNAL] Annonce updated: (owner=\"" + this.content[0] + "\", title=\"" + this.content[1] + "\", content=\"" + this.content[2] + "\", domain=\"" + this.content[3] + "\", price=" + this.content[4] + ", id=" + this.content[5] + ")";
            }
            case CLIENT_LOG_DOMAIN_LIST_OK -> {
                return "[INTERNAL] Domain list is: " + this.content[0];
            }
            case CLIENT_LOG_DOMAIN_LIST_KO -> {
                return "[INTERNAL] Failed to request domain list: (error=\"" + this.content[0] + "\")";
            }
            case CLIENT_LOG_ANNONCE_FROM_DOMAIN_OK -> {
                return "[INTERNAL] Annonces from domain are: " + this.content[0];
            }
            case CLIENT_LOG_ANNONCE_FROM_DOMAIN_KO -> {
                return "[INTERNAL] Failed request annonce from domain: (error=\"" + this.content[0] + "\")";
            }
            case CLIENT_LOG_REQUEST_PUBLIC_KEY_KO -> {
                return "[INTERNAL] Failed to request public key of server: (error=\"" + this.content[0] + "\")";
            }
            default -> {
                return "error 1";
            }
        }
    }
}
