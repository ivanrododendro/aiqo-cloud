# Linee guida Lombok

**Lingua**
Rispondi sempre in italiano

- **Logging**: i componenti che producono log applicativi utilizzano Lombok `@Slf4j` per ottenere il logger SLF4J preconfigurato. Esempio: `org.iro.aiqo.collector.logparser.parser.PlainTextLogFileParser`.
- **POJO di dominio**: tutte le strutture dati (es. `LogEntry`, `Buffers`, `Wal`, `ParseResult`) impiegano `@Data` per generare automaticamente getter, setter, `equals`, `hashCode` e `toString`. Dove opportuno viene aggiunto `@AllArgsConstructor`/`@NoArgsConstructor` per preservare i costruttori richiesti.
- **Mapping**: per la trasformazione tra DTO, entity e viste utilizzare MapStruct con interfacce `@Mapper`. Le implementazioni generate restano nella cartella build; non aggiungere manualmente mapper ad-hoc.
- **Regola d'adozione**: quando si introducono nuovi POJO o classi che richiedono logging o mapping, utilizzare sistematicamente le annotazioni Lombok corrispondenti (`@Data` per i dati, `@Slf4j` per i log) e definire mapper MapStruct per le conversioni strutturate, così da mantenere coerenza stilistica e ridurre il boilerplate.

- Ogni nuova classe ed ogni nuovo metodo deve essere accompagnato dai test unitari
- Tutti il client REST devono essere sviluppati come classi a se stanti basate su RestTemplate
- Scritture di log : una scrittura di log livello INFO per ogni metodo in entrata di uno use case, ad esempio un controller REST
- Scritture di log : deve essere possibile seguire a livello DEBUG l'esecuzione in dettaglio, quindi le scritture di log DEBUG devono permettere di coprire tutto l'albero logico dell'esecuzione, con dettagli contestuali delle variabili
- la struttura dei package : 
  - org.iro.aiqo.submitter 
    - local : tutti le classi / interfacce della collezione di file locale
    - log le classe / interfacce di alto livello comuni a ogni tipo di gestione dei log
    - logparser : i file relativi al parsing e comuni a ogni tipo di parsing
    - logparser.model : il modello in uscita del parsing
    - logparser.parser : le differenti implementazione di parser
    - api> : tutti i file Java relativi all'api di persistenza
    - api>.domain> : tutti gli oggetti relativi all'api del dominio indicato, Client, DTO, ...
    - service : i servizi cioé tutti quelle classe *Service che contengono un po' di logica e di aggregazione di chiamate di altre classi 
  - Nel pom.xml usa sempre delle proprietà per le versioni delle dipendenze
  - Preferisci sempre implementazioni Java esistenti piuttosto che implementazioni specifiche quando possibile
  - Se implementazioni java non sono disponibili verifica l'esistenza e proponi lib di terze parti
  - non aggiungere mai lib di terze parti senza chiedere
  - metti tutti i parametri in aiqo.collector.properties
  - non utlizzare mai variabili di classe come cache, non implementare meccanismi di cache applicativi a meno che non ti sia chiesto 
  - non creare package Java per meno di tre classi, solo a partire da tre classe
  - usa application.properties solo per le proprietà spring
  - usa aiqo.submitter.properties solo per le proprietà specifiche a questo progetto

Validazione
  - usa Bean Validation (JSR 380/303)  per la validazione dei parametri di metodo
  - usa java.util.Objects.requireNonNull e org.springframework.util.StringUtils.hasText per la validazione in-method

Tests
  - Utilizza Jupiter pour l'automatizzazione dei tests
  - usa la forma Given-When-Then del BDD per strutturare i test, mettendo in evidenza precondizioni, oggetto del test, e risultato atteso 
  - 