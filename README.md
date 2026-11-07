# Fantasy Folk

Esqueleto inicial de um mod para Minecraft Java Edition 26.3 usando Fabric.

O projeto contém o Goblin v0.2, a infraestrutura de comunidades v0.3, o sistema
de profissões por equipamentos v0.4, o gerenciamento comunitário v0.4.1, a IA
do Guarda v0.5, os depósitos comunitários v0.6 e o Caçador v0.7; anões,
draconatos e tieflings ainda não foram implementados.

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

## Goblin v0.2

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

## Sistema de comunidades v0.3

As comunidades são armazenadas no `SavedDataStorage` do servidor e possuem UUID,
tipo (`VANILLA` ou `CUSTOM`), nome automático, dimensão, centro, raio de 32
blocos, estado (`ACTIVE` ou `INACTIVE`) e referências persistentes aos UUIDs dos
habitantes. O vínculo não guarda referências diretas a entidades descarregadas.

### Vila vanilla

Em uma vila existente, agache-se e interaja com um sino usando a mão vazia.
O Fantasy Folk consulta apenas a área local: o sino precisa ser um ponto de
reunião, deve haver pelo menos um villager carregado e pelo menos um POI
adicional (por exemplo, cama ou estação de trabalho). Um sino isolado não é
registrado. Sem a interação especial, o sino mantém seu comportamento vanilla.

### Assentamento próprio

O item `fantasyfolk:community_marker` está na aba Criativo de blocos funcionais.
Ao posicioná-lo em uma área sem sobreposição, ele funda uma comunidade `CUSTOM`
com o bloco como centro. Dois centros iguais ou territórios sobrepostos são
recusados. Destruir o marco marca a comunidade como `INACTIVE`, preservando seus
habitantes e dados.

### Comandos temporários de desenvolvimento

Todos os comandos abaixo exigem permissão de operador:

```text
/fantasyfolk village info
/fantasyfolk village info <uuid-da-comunidade>
/fantasyfolk village list
/fantasyfolk village link <uuid-da-comunidade> <entidade-ou-seletor>
/fantasyfolk village unlink <entidade-ou-seletor>
```

Exemplo de fluxo com um Goblin:

```text
/summon fantasyfolk:goblin
/fantasyfolk village list
/fantasyfolk village link 00000000-0000-0000-0000-000000000000 @e[type=fantasyfolk:goblin,limit=1,sort=nearest]
/fantasyfolk village info
/fantasyfolk village unlink @e[type=fantasyfolk:goblin,limit=1,sort=nearest]
```

O UUID do exemplo deve ser substituído pelo valor exibido por `list`. A versão
atual aceita vínculos de Goblins; a abstração é independente de raça e será
reutilizada para Anões, Draconatos e Tieflings. Villagers vanilla nunca são
convertidos nem vinculados automaticamente.

## Sistema de profissões v0.4

As profissões são registradas em `ProfessionRegistry` e não ficam acopladas ao
Goblin. Qualquer raça futura pode implementar `ProfessionHolder` e reutilizar o
mesmo manager, a mesma confirmação e as mesmas transações de inventário.

### Profissões e equipamentos

| ID | Nome | Equipamento aceito | Slot do NPC |
| --- | --- | --- | --- |
| `fantasyfolk:guard` | Guarda | espadas vanilla (`#minecraft:swords`) | mão principal |
| `fantasyfolk:hunter` | Caçador | arco vanilla (`minecraft:bow`) | mão principal |

Não há atribuição automática. A tabela de mapeamento fica isolada para que
picaretas, machados, enxadas e varas de pesca possam ser adicionados depois sem
alterar as entidades.

### Atribuir, trocar e remover

O NPC precisa estar vinculado a uma comunidade existente. A comunidade precisa
estar `ACTIVE` para receber uma profissão nova. Uma comunidade `INACTIVE`
preserva as profissões já salvas, mas não aceita novas atribuições ou trocas.

1. Segure uma espada ou arco na mão principal.
2. Agache e interaja com um Goblin vinculado à comunidade.
3. Interaja novamente, ainda agachado, em até 10 segundos para confirmar.

A primeira interação só pede confirmação; nenhum item é consumido. Na confirmação,
uma unidade do item é transferida para a mão principal do NPC. Ao trocar, a
espada/arco anterior volta para o inventário antes de o novo item ser aplicado.
Encantamentos, durabilidade e componentes são preservados por cópia do
`ItemStack`.

Para remover, deixe a mão principal vazia, agache e interaja duas vezes. A
remoção só acontece se houver espaço seguro para devolver o equipamento. Se o
inventário estiver cheio, a operação é cancelada sem alterar o NPC nem o
inventário. A mesma regra evita perda ou duplicação durante trocas.

### Persistência e aparência

O ID da profissão e o `ItemStack` associado são salvos no NBT normal da entidade,
junto com o vínculo comunitário já existente. IDs desconhecidos de versões
futuras são mantidos com seu equipamento, sem derrubar o carregamento nem
descartar silenciosamente o item. O renderer humanoide já possui a camada
vanilla de item na mão; por isso a arma/arco aparece sem alterar a geometria,
UV, textura ou animação do Goblin v0.2.

### Roteiro de teste manual A–J

Com Java 25 configurado, execute A e depois use o cliente de desenvolvimento
para B–J:

```text
A. ./gradlew build
B. ./gradlew runClient
C. Dê um `community_marker`, coloque-o, invoque um Goblin e faça `village list`.
   Vincule-o com `/fantasyfolk village link <uuid> @e[type=fantasyfolk:goblin,limit=1,sort=nearest]`.
D. Segure uma espada vanilla; agache e interaja duas vezes. Confirme Guarda,
   mão principal e redução de exatamente uma unidade.
E. Verifique visualmente a espada na mão do Goblin e repita a interação: nada
   deve ser duplicado.
F. Segure um arco e repita a confirmação. A espada anterior deve voltar e o
   Goblin deve ficar Caçador com o arco.
G. Esvazie a mão, repita a confirmação e verifique que arco e profissão somem
   do NPC, enquanto o arco volta ao inventário.
H. Teste arco/espada com encantamento ou dano: os componentes devem permanecer.
I. Teste confirmação expirada, item trocado, outro NPC e inventário cheio:
   nenhuma tentativa inválida deve mover item.
J. Saia/reentre ou descarregue/recarregue o chunk; depois quebre o marco e
   confirme que a comunidade fica inativa, sem apagar a profissão existente.
```

Para testar a rejeição de comunidade, invoque um Goblin sem executar o comando
`village link`: a confirmação não deve atribuir a profissão e o item deve
continuar na mão do jogador. Para testar a troca com inventário cheio, deixe o
NPC já profissionalizado e preencha os slots de inventário com itens não
empilháveis antes de oferecer o novo equipamento.

### Limitações conhecidas

- A detecção de vila é sob demanda e não faz uma busca global. Ela considera
  apenas POIs e villagers disponíveis na área carregada no momento da interação.
- A perda do sino vanilla é reconciliada de forma localizada quando a comunidade
  é consultada (`info`/`list`) e o chunk do centro está carregado; não há varredura
  contínua do mundo.
- Não há recuperação ou transferência automática de centros inativos.
- Apenas Guarda e Caçador estão implementados; as outras profissões previstas
  são somente pontos de extensão da tabela de equipamentos.
- A interação atual é um fluxo de mensagens, sem interface gráfica.
- O teste manual completo requer um cliente Minecraft; os testes automatizados
  cobrem registro/mapeamento disponível no ambiente de teste e persistência de
  componentes.

## Goblin Guarda — v0.5

Um Goblin com a profissão `fantasyfolk:guard`, uma espada equipada e vínculo com
uma comunidade `ACTIVE` patrulha e defende o território no servidor. Para
atribuir a profissão, selecione a comunidade, vincule o Goblin, segure uma espada,
agache-se e interaja duas vezes em até 10 segundos.

### Patrulha e retorno

Sem ameaças, o Guarda escolhe destinos navegáveis em até 24 blocos horizontais
do centro, ou no raio real da comunidade quando ele for menor. O destino e o
caminho precisam permanecer dentro do território. A navegação vanilla rejeita
água, blocos sólidos, posições com penalidade e caminhos inalcançáveis; cada
tentativa tem tempo limitado. Se o Guarda sair do território, ele procura um
caminho de volta em etapas, sem teletransporte ou carregamento artificial de
chunks.

### Ameaças e defesa

O Guarda procura, a cada intervalo limitado, ameaças carregadas em até 12 blocos:

- zumbis, husks, afogados e zombie villagers;
- esqueletos, strays, bogged e demais variantes de `AbstractSkeleton`;
- pillagers.

Zombified Piglins, Endermen, Creepers, jogadores, villagers, golens e Goblins
aliados não são atacados proativamente. Aranhas só são consideradas quando
atacam o próprio Guarda, um habitante Fantasy Folk vinculado à mesma comunidade
ou um villager protegido dentro do território.

O combate reutiliza o Goal corpo a corpo vanilla, respeita o intervalo de ataque,
os atributos e encantamentos da espada e aplica desgaste ao item realmente
equipado. O dano base do Goblin permanece em 4 pontos antes dos modificadores do
equipamento. A perseguição termina quando o alvo morre, sai do território, fica
inalcançável, some, excede o tempo de combate ou quando a comunidade deixa de ser
operacional. Depois disso, o Guarda retorna e retoma a patrulha.

### Interromper a atividade

Trocar a espada por um arco transforma o Goblin em Caçador e cancela imediatamente
alvo, navegação e agressividade de Guarda. Remover a profissão produz o mesmo
efeito. O vínculo comunitário é preservado. Destruir o marco ou remover o vínculo
suspende a IA profissional, mas mantém profissão e equipamento salvos. Reatribuir
Guarda reativa os mesmos Goals, sem registros duplicados.

### Limitações conhecidas

- A qualidade da patrulha depende do pathfinder vanilla; em terreno Amplified o
  Guarda pode aguardar e tentar outro destino quando não encontra rota segura.
- A defesa observa apenas entidades carregadas e próximas; não existe varredura
  global, carregamento de chunks ou coordenação tática entre Guardas.
- Creepers são ignorados nesta versão para evitar explosões dentro da comunidade.
- Não há proteção contra jogadores, guerras entre comunidades ou IA de Caçador.

### Teste dentro do jogo

```text
1. Crie e selecione uma comunidade com o Registro Comunitário.
2. Invoque um Goblin, vincule-o e atribua uma espada.
3. Observe a patrulha e invoque um zumbi a menos de 12 blocos.
4. Leve o alvo para fora do território e confirme o retorno do Guarda.
5. Faça um hostil atacar outro Goblin vinculado ou um villager dentro da área.
6. Teste Creeper, Enderman, aliados e golem para confirmar que são ignorados.
7. Troque a espada por arco e depois remova a profissão; alvo e patrulha devem parar.
8. Quebre o marco, salve/reabra o mundo e teste novamente em terreno montanhoso.
```

## Sistema de Depósitos Comunitários — v0.6

Depósitos são registros persistentes de baús vanilla ou barris vanilla. O
registro guarda apenas a comunidade, a dimensão, as coordenadas, o tipo e o
estado do recipiente; os itens permanecem exclusivamente no inventário vanilla
do bloco. Baús do End, caixas de shulker, fornalhas, funis e outros blocos não
são aceitos.

### Registrar, consultar e remover

1. Use o Registro Comunitário em um Marco de Comunidade ou sino para selecionar
   uma comunidade. Como atalho, use o Registro no ar dentro do território: ele
   selecionará a comunidade ativa mais próxima. Se o Registro não tiver seleção,
   um baú ou barril dentro do território também pode inferir essa comunidade no
   primeiro contato.
2. Aproxime-se de um baú ou barril dentro do território e interaja com ele
   segurando o Registro. O recipiente não será aberto; a primeira interação
   arma a confirmação.
3. Interaja novamente, sem precisar agachar, em até 10 segundos para confirmar o cadastro; após o
   sucesso, a comunidade fica selecionada no Registro.
4. Use o Registro novamente sem agachar para consultar tipo, estado, espaços
   ocupados e capacidade real. Um baú duplo aparece como um depósito único com
   capacidade conjunta.
5. Para remover o vínculo, agache e interaja duas vezes com o depósito usando o
   Registro da comunidade correspondente. O Shift é exigido apenas para remoção.

A remoção não destrói o recipiente nem altera os itens. O cadastro verifica no
servidor a comunidade selecionada, sua dimensão, estado, território, permissões
e propriedade do recipiente. Sem o Registro, baús e barris mantêm a abertura e o
comportamento vanilla normais. Nenhum comando administrativo é necessário para
esses fluxos.

### Inventário de trabalho e entrega

Cada Goblin possui nove espaços de inventário de trabalho, separados da espada
ou arco da profissão. Segure um item comum na mão principal e interaja sem
agachar com um Goblin vinculado a uma comunidade ativa. Uma unidade só é
removida do jogador quando cabe no inventário do Goblin. Espadas, arcos,
Registro Comunitário e alimentos não são consumidos por essa interação.

O inventário de trabalho é salvo junto com a entidade, sobrevive a troca de
profissão, desvinculação, reinicialização e recarga do chunk. Na morte do
Goblin, seus itens de trabalho são descartados no mundo uma única vez; o
equipamento profissional continua obedecendo às regras existentes.

### Transporte automático

Quando houver itens de trabalho, comunidade ativa e um depósito registrado com
espaço, o Goblin escolhe um recipiente acessível, caminha até ele usando a
navegação vanilla e transfere apenas a quantidade realmente aceita. Não há
teletransporte, carregamento artificial de chunks, retirada de itens dos
depósitos ou descarte automático quando todos estão cheios. Se um depósito
ficar inválido, for destruído ou estiver cheio, outro depósito pode ser tentado;
caso nenhum esteja disponível, os itens ficam no inventário do Goblin.

Para o Guarda, defesa e combate têm prioridade sobre a entrega. Após a ameaça
ser resolvida, a rotina de depósito pode ser retomada. Comunidades inativas
preservam seus registros e conteúdos, mas suspendem o transporte automático.

### Limitações conhecidas

- A reconciliação de um baú duplo usa as posições e o estado atual dos dois
  blocos; se uma metade for removida, o registro acompanha a metade restante.
- Um registro inválido é preservado para diagnóstico e não é reativado
  automaticamente; um novo recipiente precisa ser cadastrado novamente.
- A navegação depende do pathfinder vanilla e pode aguardar em terreno
  Amplified ou diante de um depósito inacessível.
- A entrega manual é propositalmente simples; não existe atividade de
  Minerador, Lenhador ou Agricultor.
- A capacidade exibida é a quantidade de slots, não a capacidade efetiva de uma
  pilha específica; a transferência real considera pilhas, componentes e
  durabilidade.

### Teste manual A–O

```text
A. Crie uma comunidade, selecione-a no Registro e cadastre um baú sem comandos.
B. Repita o cadastro usando um barril.
C. Consulte o depósito e remova o vínculo; confirme que os itens continuam lá.
D. Vincule um Goblin e entregue sticks ou outro item comum sem agachar.
E. Confirme que uma unidade sai do jogador apenas quando o inventário aceita.
F. Observe o Goblin caminhando até o depósito e armazenando no bloco real.
G. Encha um depósito e confirme que os itens restantes ficam no Goblin.
H. Cadastre um segundo depósito e confirme a tentativa alternativa.
I. Salve, reabra e recarregue o chunk; valide depósitos e inventário de trabalho.
J. Desative a comunidade e confirme que os registros permanecem sem entregas.
K. Destrua um depósito e confirme que nenhum Goblin tenta acessar o bloco.
L. Durante uma entrega, aproxime um zumbi e confirme a prioridade do Guarda.
M. Teste dois Goblins ou jogadores usando o mesmo depósito sem duplicação.
N. Registre um baú duplo, altere sua configuração e confirme a reconciliação.
O. Repita o fluxo completo em um servidor dedicado sem comandos administrativos.
```

## Goblin Caçador — v0.7

O Caçador só trabalha quando o Goblin possui a profissão `fantasyfolk:hunter`,
um arco válido na mão principal, vínculo com uma comunidade `ACTIVE` na mesma
dimensão e uma área de caça ativa. Sem área configurada ou sem flechas, ele
mantém a profissão e o equipamento, mas não caça.

### Configurar a área pelo Registro

1. Selecione a comunidade no Registro Comunitário usando um Marco, sino ou o
   fluxo de seleção já existente.
2. Vá até um bloco comum de grama, terra ou pedra dentro do território.
3. Agache e use o Registro; repita a interação no mesmo bloco em até 10
   segundos para confirmar.
4. Use o Registro novamente no novo ponto para mover a área. No centro atual,
   o mesmo fluxo de confirmação remove a área.

Cada comunidade possui no máximo uma área persistente, com raio operacional de
12 blocos horizontais. A área efetiva é a interseção desse círculo com o
território da comunidade. O Registro informa se a área está configurada e suas
coordenadas; nenhuma dessas operações requer comandos administrativos.

### Caça, segurança e limites

O allow-list inicial contém vacas, porcos, galinhas, ovelhas e coelhos. O
Caçador exclui filhotes, animais nomeados, domesticados, presos por coleira,
montados/transportados, animais em cercados reconhecíveis e habitantes de
outros sistemas comunitários. A seleção e as consultas consideram somente
entidades carregadas. A política também exige pelo menos dois adultos elegíveis
da espécie na área antes de selecionar uma presa.

O Goblin usa navegação vanilla limitada, permanece no círculo efetivo, mantém
linha de visão e dispara uma flecha real. O arco equipado é o `ItemStack` real:
encantamentos, componentes e durabilidade são preservados. Cada tiro consome
uma flecha entregue no inventário de trabalho, e as flechas ficam reservadas
para a profissão em vez de serem depositadas como loot. A animação usa o estado
de carga de braços compatível com o modelo Piglin/Goblin existente, sem mudar
geometria, UV ou textura.

Cada Caçador pode abater no máximo quatro animais por dia do Minecraft. Após a
morte, os drops vanilla reais são rastreados de modo conservador pela posição e
janela temporal do abate, recolhidos no inventário de trabalho de nove espaços
e enviados aos depósitos já registrados. Itens que não cabem permanecem no
mundo ou no Goblin; não são recriados nem descartados automaticamente.

Monstros próximos interrompem a rotina e provocam uma retirada curta, sem
transformar o Caçador em Guarda. Troca ou remoção da profissão cancela alvo,
disparo, mira e navegação imediatamente. O contador diário, o dia de referência,
a área, o equipamento, as flechas e o inventário de trabalho sobrevivem a
salvar/reabrir o mundo.

### Limitações conhecidas

- A proteção contra cercados é heurística: reconhece concentrações de cercas,
  portões e muros próximos, mas não pretende classificar todas as fazendas.
- A atribuição de drops é deliberadamente conservadora; drops vanilla fora da
  lista conhecida ou itens que se fundem com entidades pré-existentes podem não
  ser coletados, evitando recolher itens de jogadores.
- A proteção da trajetória bloqueia entidades vivas encontradas entre o arco e
  o alvo, mas a física posterior da flecha ainda pode sofrer acidentes fora da
  linha verificada.
- O pathfinder pode falhar ou esperar em terreno Amplified, desníveis extremos
  ou destinos inacessíveis; não há teletransporte nem carregamento artificial de
  chunks.
- Não há reabastecimento automático de flechas a partir dos depósitos.

### Teste manual

```text
1. Crie e selecione uma comunidade, vincule um Goblin e atribua um arco.
2. Configure uma área de caça em terreno comum e consulte o Registro para
   confirmar coordenadas e raio.
3. Entregue flechas pelo inventário de trabalho e coloque pelo menos dois
   adultos de uma espécie elegível na área.
4. Observe a aproximação, a animação de mira, a flecha real, os drops e a
   entrega no depósito cadastrado.
5. Teste filhote, animal nomeado, coleira, cercado, jogador na trajetória e um
   monstro próximo.
6. Gaste quatro abates, salve/reabra e confirme o bloqueio até o próximo dia.
7. Mova/remova a área, troque para Guarda e remova a profissão; a caça deve
   parar sem perder arco, flechas ou loot.
```

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
