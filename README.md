# Nexus — Reimagined

Aplicativo Android nativo em Kotlin e Jetpack Compose. História, elenco, Comic Vine e splash em vídeo mantidos. O código da história veio do arquivo anexado nesta conversa; apenas a visibilidade de SceneCanvas foi alterada para permitir a visualização de memórias.

## Aplicar ao seu aplicativo funcionando

Esta é a opção recomendada. Como o ZIP mais recente não pôde ser enviado, este pacote usa a última base disponível. Ele não contém as imagens da história que você adicionou posteriormente no seu computador.

1. Extraia o pacote em uma pasta diferente da pasta do seu aplicativo atual.
2. Feche a execução do aplicativo no Android Studio.
3. No PowerShell, dentro da pasta extraída Nexus, execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\APLICAR-DESIGN.ps1 -ProjetoAtual "C:\caminho\do\seu\Nexus"
```

O script cria backup dos arquivos anteriores e aplica somente dez arquivos Kotlin. Não copia, remove ou renomeia assets; não modifica episode_1.json, characters.json, asset_aliases.json, API, configuração do Gradle ou os dados salvos. A política Bypass se aplica somente a esse processo. Leia o script antes de executá-lo.

4. Abra seu projeto original no Android Studio e execute normalmente. Não é necessário desinstalar o aplicativo.

Alternativa manual: copie MainActivity.kt e os nove arquivos de ui citados no script para as mesmas pastas do projeto atual. Mantenha seus demais arquivos.

## Abrir este pacote como projeto separado

Antes de executar, copie **todo o conteúdo da pasta app/src/main/assets do seu projeto atual** para app/src/main/assets deste pacote, substituindo os arquivos antigos. Isso inclui seus JSONs de roteiro e mapeamento, todas as imagens e o vídeo. A cópia é necessária para que a história use as mesmas imagens da versão que já funciona no seu celular.

Mantenha sua chave COMIC_VINE_API_KEY em local.properties. O pacote não inclui sua chave nem o caminho do seu SDK. Use JDK 17 e Android SDK 35.

## Experiência

- Home editorial com três capítulos, imagens amplas, parallax, avanço automático a cada sete segundos e controle de pausa.
- Faixa inferior do episódio com uma cópia desfocada da própria imagem; texto e botões permanecem nítidos. No Android anterior ao 12, o sistema usa a camada translúcida sem o desfoque nativo do Compose.
- Fundos luminosos animados, superfícies translúcidas e navegação flutuante.
- Elenco com pôsteres, filtro de favoritos e fichas com seção clara de poderes.
- Cartão dimensional na ficha: toque para virar em perspectiva 3D, arraste para inclinar.
- Laboratório: anéis calculados em coordenadas 3D, projeção em perspectiva, partículas animadas, rotação por arraste, pausa, quatro cores, seleção do elenco e favoritos.
- Mapa do percurso: somente cenas já alcançadas, diálogos legíveis e prévia das cenas sem alterar seu progresso.
- Diário com compartilhamento de decisões pelo seletor nativo do Android. Nada é enviado automaticamente.
- Redução de movimento, velocidade de diálogo, salvamento e busca Comic Vine continuam disponíveis. As animações contínuas pausam quando o aplicativo vai para o fundo.

Os personagens continuam usando seus assets 2D. O 3D está na geometria procedural do portal e nas transformações dos cartões; nenhum modelo GLB foi inventado ou substituído por outra imagem.

## Suas imagens

Nenhum asset recebido foi renomeado ou reprocessado. A história continua lendo seu asset_aliases.json e os limites e rotações de episode_1.json. O vídeo continua em app/src/main/assets/video/splash_nexus.mp4.

Nos pôsteres, o código prefere images/catalog_miles.png, images/catalog_gwen.png e images/catalog_peter.png **somente se existirem**. Caso contrário, usa o portrait existente; depois, a imagem do personagem. Para melhorar a resolução, exporte o pôster em tamanho maior e use um desses nomes opcionais. Isso não altera os arquivos da história. Uma miniatura pequena não ganha detalhes por código.

## Validação

O roteiro de 53 cenas, destinos de escolhas, integridade dos assets disponíveis, preservação do código anexado e distribuição do pacote foram verificados localmente. Execute python3 tools/validate_project.py para repetir essas verificações estruturais.

A compilação Android e a execução em aparelho não puderam ser realizadas neste ambiente: não há Android SDK instalado e o download do Gradle foi bloqueado por falta de acesso à rede. O ZIP contém código-fonte, não um APK certificado como compilado. No Android Studio, execute Build > Make Project e teste no aparelho antes de distribuir.

Referências de direção visual: instruções Apple fornecidas por você e organização editorial por capítulos de https://hubtown.co.in/. Adaptadas ao Nexus para celular, mantendo sua identidade escura e seu conteúdo.
