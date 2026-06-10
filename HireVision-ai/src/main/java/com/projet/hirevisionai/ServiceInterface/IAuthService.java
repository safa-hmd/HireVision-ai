package com.projet.hirevisionai.ServiceInterface;


import com.projet.hirevisionai.Dto.*;
import com.projet.hirevisionai.Entity.User;

public interface IAuthService {
    User register(RegisterRequest req);

    AuthResponse login(LoginRequest req);
    AuthResponse completeGoogleRegister(CompleteGoogleRegisterRequest req);

    void forgotPassword(ForgotPasswordRequest req);
    void resetPassword(ResetPasswordRequest req);
}