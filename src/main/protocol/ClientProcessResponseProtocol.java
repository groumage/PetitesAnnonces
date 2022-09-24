package main.protocol;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface ClientProcessResponseProtocol {

    void requestPublicKeyOk(Request req) throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException;
    void requestPublicKeyKo(Request req);
    void signUpOk(Request req);
    void signUpKo(Request req);
    void signInOk(Request req);
    void signInKo(Request req);
    void signOutOk();
    void signOutKo();
    void createAnnonceOk(Request req);
    void createAnnonceKo(Request req);
    void updateAnonceOk(Request req);
    void updateAnonceKo(Request req);
    void domainListOk(Request req);
    void domainListKo(Request req);
    void annonceFromDomainOk(Request req);
    void annonceFromDomainKo(Request req);
    void removeAnnonceOk(Request req);
    void removeAnnonceKo(Request req);
}
