# familie-ef-personhendelse

App som lytter på hendelser i PDL og oppretter oppgaver til oppfølging av saksbehandlere for relevante hendelser.

Appen er eid av enslig forsørger i Team Familie, og kan nås på Slack i kanalen #team-familie

## Hendelser

Applikasjonen oppretter en oppgave av typen "vurder livshendelse", gitt at brukeren har en løpende stønad.
En allerede opprettet oppgave oppdateres hvis det for eksempel oppstår en korreksjon. Personhendelser det opprettes oppgaver av er oppsummert i tabellen nedenfor.

| Personhendelse | Beskrivelse                                                                                                                                                                                          |  
| ---- |------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|  
| Sivilstand | Oppretter oppgave hvis endringen i sivilstand ikke er skilsmisse, separasjon, eller opphør.                                                                                                          |  
| Fødsel | Oppretter oppgave hvis bruker har fått nye barn som ikke finnes på behandling, eller når terminbarn er født i måneden før eller etter termindato.                                                    |  
| Utflytting fra Norge | Oppretter oppgave hvis personen har flyttet ut av landet.                                                                                                                                            |  
| Dødsfall | Oppretter oppgave hvis det har oppstått dødsfall ifm person eller fødsel.                                                                                                                            |  
| Opphør, annulering, og korreksjon | Hvis det er registrert en tidligere personhendelse, og hvis tilknyttet oppgave er åpen, så oppdateres tilhørende oppgave hvis endringstypen er opphør, annulering, eller korreksjon. Dette gjelder ikke sivilstand. |  

##### Kode generert av GitHub Copilot

Dette repoet kan ha brukt GitHub Copilot til å generere kode.