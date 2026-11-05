# Fantasy Folk

Esqueleto inicial de um mod para Minecraft Java Edition 26.3 usando Fabric.

Esta etapa prepara o projeto e o ciclo de inicialização do mod. O protótipo
Goblin v0.1 está implementado; anões, draconatos e tieflings ainda não foram
implementados.

## Ambiente

O projeto requer Java 25. Para usar o JDK 25 instalado localmente sem alterar
as configurações globais do sistema:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

O Gradle do projeto é fornecido pelo Wrapper. Não é necessário usar o Gradle
instalado globalmente.

## Comandos de desenvolvimento

```bash
# Compilar e executar verificações
./gradlew build
./gradlew test

# Iniciar o cliente de desenvolvimento
./gradlew runClient

# Gerar o JAR em build/libs/
./gradlew jar
```

O comando `build` também gera o JAR em `build/libs/`. A pasta `run/` contém
os dados locais da execução de desenvolvimento e não deve ser versionada.

## Goblin v0.1

O Goblin é uma entidade independente registrada como `fantasyfolk:goblin`.
Ele possui 20 pontos de vida, velocidade `0.28`, comportamento neutro,
movimentação aleatória e observa jogadores próximos. A renderização reutiliza
a geometria e o modelo de animação do Piglin adulto, com escala visual de
`85%` e a textura `assets/fantasyfolk/textures/entity/goblin.png`.

Para invocá-lo no jogo:

```text
/summon fantasyfolk:goblin
```

O ovo de spawn está na aba Criativo de ovos de spawn. O spawn natural está
desativado nesta versão.

## Estrutura inicial

- `com.fantasyfolk.FantasyFolk`: entrypoint comum do mod.
- `com.fantasyfolk.entity`: entidades e raças.
- `com.fantasyfolk.client`: código exclusivo do cliente.
- `com.fantasyfolk.registry`: registros do mod.
- `com.fantasyfolk.world`: integração com o mundo e geração futura.

## Versões

- Minecraft: `26.3`
- Java: `25`
- Fabric Loader: `0.20.1`
- Fabric API: `0.165.0+26.3`
- Fabric Loom: `1.19-SNAPSHOT`
- Gradle Wrapper: `9.8.2`
