package util; 

import java.io.File; // Permite receber o caminho do arquivo que guarda a chave.
import java.io.IOException; // Representa erros de leitura e gravação de arquivos.
import java.nio.file.Files; // Oferece métodos para ler e gravar arquivos.
import java.nio.file.StandardOpenOption; // Define as opções usadas ao gravar um arquivo.
import java.security.InvalidAlgorithmParameterException; // Indica parâmetros inválidos para um algoritmo criptográfico.
import java.security.InvalidKeyException; // Indica que a chave usada não é válida.
import java.security.KeyStoreException; // Representa erros relacionados a armazenamento de chaves.
import java.security.NoSuchAlgorithmException; // Indica que um algoritmo solicitado não está disponível.
import java.security.SecureRandom; // Gera valores aleatórios adequados para uso criptográfico.
import java.security.cert.CertificateException; // Representa erros relacionados a certificados.
import java.util.Arrays; // Fornece operações para copiar e comparar arrays.
import javax.crypto.BadPaddingException; // Indica erro de autenticação ou preenchimento ao decifrar.
import javax.crypto.Cipher; // Executa as operações de cifragem e decifragem.
import javax.crypto.IllegalBlockSizeException; // Indica um tamanho de bloco incompatível com a operação.
import javax.crypto.KeyGenerator; // Gera chaves criptográficas.
import javax.crypto.NoSuchPaddingException; // Indica que o preenchimento solicitado não está disponível.
import javax.crypto.SecretKey; // Representa uma chave criptográfica secreta.
import javax.crypto.spec.GCMParameterSpec; // Configura o nonce e o tamanho da etiqueta do AES-GCM.
import javax.crypto.spec.SecretKeySpec; // Cria uma chave AES a partir de bytes.

/**
 * Cifra e decifra dados com AES-GCM. O formato armazenado inclui identificação
 * de versão e nonce para que a autenticação seja verificada na leitura.
 */
public class CryptoAES { // Declara a classe responsável pelas operações com AES.
    private static final byte[] FORMAT_MAGIC = {'C', 'A', 'E', 'S'}; // Identifica o formato dos dados cifrados desta classe.
    private static final byte FORMAT_VERSION = 1; // Indica a versão atual do formato de dados cifrados.
    private static final int KEY_SIZE_BITS = 256; // Define o tamanho da chave AES em bits.
    private static final int KEY_SIZE_BYTES = KEY_SIZE_BITS / 8; // Converte o tamanho da chave de bits para bytes.
    private static final int NONCE_SIZE_BYTES = 12; // Define o tamanho recomendado do nonce para AES-GCM.
    private static final int TAG_SIZE_BITS = 128; // Define o tamanho da etiqueta de autenticação em bits.
    private static final int HEADER_SIZE_BYTES = FORMAT_MAGIC.length + 1; // Soma os bytes identificadores e o byte da versão.
    private static final int ENCRYPTED_HEADER_SIZE_BYTES = HEADER_SIZE_BYTES + NONCE_SIZE_BYTES; // Calcula o espaço do cabeçalho seguido pelo nonce.
    private static final int TAG_SIZE_BYTES = TAG_SIZE_BITS / 8; // Converte o tamanho da etiqueta de bits para bytes.

    private byte[] textoCifrado; // Guarda o resultado mais recente da cifragem.
    private byte[] textoDecifrado; // Guarda o resultado mais recente da decifragem.

    public CryptoAES() { // Cria uma instância sem resultados de cifragem ou decifragem.
        textoCifrado = null; // Inicializa o resultado cifrado como inexistente.
        textoDecifrado = null; // Inicializa o resultado decifrado como inexistente.
    } 

    /**
     * Gera uma chave AES-256 e grava seus bytes em um arquivo novo.
     * @param fSim destino da chave; a operação não sobrescreve arquivo existente
     * @throws IOException se não for possível criar ou gravar o arquivo
     */
    public void geraChave(File fSim) // Gera uma chave AES e recebe o caminho onde salvá-la.
            throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, // Declara erros possíveis ao gravar ou configurar a geração da chave.
            CertificateException, KeyStoreException { // Declara também erros de certificados e armazenamento de chaves.
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES"); // Obtém o gerador de chaves do algoritmo AES.
        keyGenerator.init(KEY_SIZE_BITS, new SecureRandom()); // Configura o gerador para criar uma chave AES-256 com aleatoriedade segura.
        SecretKey secretKey = keyGenerator.generateKey(); // Gera a chave secreta AES.

        Files.write( // Grava os bytes da chave no arquivo indicado.
                fSim.toPath(), // Converte o arquivo recebido em um caminho utilizável pela API NIO.
                secretKey.getEncoded(), // Obtém os bytes que representam a chave gerada.
                StandardOpenOption.CREATE_NEW, // Cria um arquivo novo e falha se ele já existir.
                StandardOpenOption.WRITE); // Abre o arquivo para gravação.
    } 

    /**
     * Cifra os dados com AES-GCM e guarda no resultado o cabeçalho, nonce
     * aleatório e etiqueta autenticadora necessários à leitura posterior.
     * @param texto bytes originais a cifrar
     * @param fSim arquivo da chave AES
     */
    public void geraCifra(byte[] texto, File fSim) // Cifra os bytes recebidos usando a chave do arquivo.
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, // Declara falhas possíveis ao selecionar algoritmo, preenchimento ou chave.
            IllegalBlockSizeException, BadPaddingException, // Declara falhas possíveis no processamento dos blocos e na autenticação.
            InvalidAlgorithmParameterException, IOException, ClassNotFoundException { // Declara falhas possíveis nos parâmetros e no acesso ao arquivo.
        textoCifrado = null; // Limpa eventual resultado antigo antes de iniciar a operação.
        if (texto == null) { // Verifica se a entrada recebida existe.
            throw new IllegalArgumentException("Texto para cifrar não pode ser nulo."); // Rejeita a entrada nula com uma mensagem explícita.
        } 

        byte[] nonce = new byte[NONCE_SIZE_BYTES]; // Reserva espaço para o nonce exclusivo desta cifragem.
        new SecureRandom().nextBytes(nonce); // Preenche o nonce com bytes aleatórios criptograficamente seguros.

        byte[] header = createHeader(); // Cria o cabeçalho que identifica o formato dos dados.
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Obtém o AES no modo GCM, que cifra e autentica os dados.
        cipher.init( // Inicializa a operação de cifragem.
                Cipher.ENCRYPT_MODE, // Define que a operação será de cifragem.
                new SecretKeySpec(readKey(fSim), "AES"), // Lê a chave do arquivo e a associa ao algoritmo AES.
                new GCMParameterSpec(TAG_SIZE_BITS, nonce)); // Configura o tamanho da etiqueta e o nonce gerado.
        cipher.updateAAD(header); // Autentica o cabeçalho sem cifrá-lo.
        byte[] encrypted = cipher.doFinal(texto.clone()); // Cifra uma cópia dos dados e acrescenta a etiqueta de autenticação.

        textoCifrado = new byte[ENCRYPTED_HEADER_SIZE_BYTES + encrypted.length]; // Reserva espaço para cabeçalho, nonce, dados cifrados e etiqueta.
        System.arraycopy(header, 0, textoCifrado, 0, header.length); // Copia o cabeçalho para o início do resultado.
        System.arraycopy(nonce, 0, textoCifrado, HEADER_SIZE_BYTES, nonce.length); // Coloca o nonce logo após o cabeçalho.
        System.arraycopy(encrypted, 0, textoCifrado, ENCRYPTED_HEADER_SIZE_BYTES, encrypted.length); // Acrescenta os dados cifrados e a etiqueta após o nonce.
    } 

    /** @return cópia dos dados cifrados mais recentes, ou {@code null} antes de cifrar */
    public byte[] getTextoCifrado() throws Exception { // Retorna os dados cifrados ou declara que a operação pode falhar.
        return copy(textoCifrado); // Devolve uma cópia para impedir que o chamador altere o array interno.
    } 

    /**
     * Valida o cabeçalho e autentica os dados cifrados antes de disponibilizar
     * o texto original; qualquer adulteração invalida a operação.
     * @param texto sequência produzida por {@link #geraCifra(byte[], File)}
     * @param fSim arquivo da mesma chave AES usada na cifragem
     */
    public void geraDecifra(byte[] texto, File fSim) // Decifra e autentica os bytes recebidos usando a chave do arquivo.
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, // Declara falhas possíveis ao selecionar algoritmo, preenchimento ou chave.
            IllegalBlockSizeException, BadPaddingException, // Declara falhas possíveis nos blocos e na autenticação dos dados.
            InvalidAlgorithmParameterException, IOException, ClassNotFoundException { // Declara falhas possíveis nos parâmetros e no acesso ao arquivo.
        textoDecifrado = null; // Limpa eventual resultado antigo antes de iniciar a operação.
        if (texto == null || texto.length < ENCRYPTED_HEADER_SIZE_BYTES + TAG_SIZE_BYTES) { // Verifica se os dados contêm ao menos cabeçalho, nonce e etiqueta.
            throw new IllegalArgumentException("Texto cifrado ausente ou incompleto."); // Rejeita entradas nulas ou curtas demais.
        } 

        byte[] header = Arrays.copyOf(texto, HEADER_SIZE_BYTES); // Extrai uma cópia dos bytes do cabeçalho.
        if (!isSupportedHeader(header)) { // Confere se os identificadores e a versão são reconhecidos.
            throw new IllegalArgumentException("Formato de texto cifrado inválido ou não suportado."); // Rejeita dados de outro formato ou de versão desconhecida.
        } 

        byte[] nonce = Arrays.copyOfRange(texto, HEADER_SIZE_BYTES, ENCRYPTED_HEADER_SIZE_BYTES); // Extrai o nonce localizado após o cabeçalho.
        byte[] encrypted = Arrays.copyOfRange(texto, ENCRYPTED_HEADER_SIZE_BYTES, texto.length); // Extrai o conteúdo cifrado e sua etiqueta de autenticação.

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); // Obtém o AES-GCM usado para decifrar e autenticar os dados.
        cipher.init( // Inicializa a operação de decifragem.
                Cipher.DECRYPT_MODE, // Define que a operação será de decifragem.
                new SecretKeySpec(readKey(fSim), "AES"), // Lê do arquivo e configura a chave AES.
                new GCMParameterSpec(TAG_SIZE_BITS, nonce)); // Configura o tamanho da etiqueta e usa o nonce extraído.
        cipher.updateAAD(header); // Fornece o cabeçalho autenticado durante a cifragem.
        textoDecifrado = cipher.doFinal(encrypted); // Valida a etiqueta e decifra os dados; falha se houver adulteração.
    } 

    /** @return cópia dos dados decifrados mais recentes, ou {@code null} antes de decifrar */
    public byte[] getTextoDecifrado() throws Exception { // Retorna os dados decifrados ou declara que a operação pode falhar.
        return copy(textoDecifrado); // Devolve uma cópia para proteger o array interno contra alterações externas.
    } 

    private static byte[] readKey(File keyFile) throws IOException { // Lê e valida a chave AES no arquivo informado.
        byte[] key = Files.readAllBytes(keyFile.toPath()); // Carrega integralmente os bytes do arquivo da chave.
        if (key.length != KEY_SIZE_BYTES) { // Confere se a chave possui exatamente 32 bytes.
            throw new IOException("Arquivo de chave inválido: esperado AES-256 (32 bytes)."); // Informa claramente o formato de chave esperado.
        } 
        return key; // Devolve os bytes da chave validada.
    } 

    private static byte[] createHeader() { // Monta o cabeçalho que identifica o formato dos dados cifrados.
        byte[] header = Arrays.copyOf(FORMAT_MAGIC, HEADER_SIZE_BYTES); // Copia os identificadores e reserva espaço para o byte da versão.
        header[FORMAT_MAGIC.length] = FORMAT_VERSION; // Grava a versão do formato após os bytes identificadores.
        return header; // Devolve o cabeçalho pronto para ser armazenado e autenticado.
    } 

    private static boolean isSupportedHeader(byte[] header) { // Verifica se o cabeçalho pertence a um formato suportado.
        if (header.length != HEADER_SIZE_BYTES || header[FORMAT_MAGIC.length] != FORMAT_VERSION) { // Rejeita tamanho incorreto ou versão diferente.
            return false; // Informa que o cabeçalho não é suportado.
        } 
        for (int i = 0; i < FORMAT_MAGIC.length; i++) { // Percorre cada byte identificador esperado.
            if (header[i] != FORMAT_MAGIC[i]) { // Compara o byte recebido com o correspondente identificador.
                return false; // Rejeita o cabeçalho assim que encontra uma diferença.
            } 
        } 
        return true; // Confirma que o cabeçalho tem identificador e versão reconhecidos.
    } 

    private static byte[] copy(byte[] value) { // Cria uma cópia defensiva de um array de bytes.
        return value == null ? null : value.clone(); // Preserva null ou devolve uma cópia independente do array.
    } 
} 
