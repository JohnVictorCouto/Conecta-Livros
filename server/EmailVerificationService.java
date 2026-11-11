package server;

/**
 * Envia códigos de verificação de cadastro usando a conta de e-mail configurada
 * no Servidor, sem expor as credenciais SMTP ao Cliente.
 */
final class EmailVerificationService {
    private EmailVerificationService() {}

    static void sendCode(String recipient, String code) throws Exception {
        EmailNotificationService.send(recipient, "Código de verificação - Conecta Livros",
            "Seu código de verificação para criar uma conta no Conecta Livros é: "
                + code + "\n\nO código expira em 10 minutos.");
    }
}
