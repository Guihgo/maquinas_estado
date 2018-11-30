typedef enum {  NOITE,DIA } S;

S estadoAtual;
int entradaAtual;
int saida;

int cSensibilidade = 850;

void setup() {
  // Inicia serial à 9600bps
  Serial.begin(9600);

  //Inicia estado atual como NOITE (S0)
  estadoAtual = NOITE;

  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {
  //Recebe entradaAtual da leitura analogica do sensor LDR
  entradaAtual = analogRead(A0);

  //atualizamos o novo estado com fs
  estadoAtual = fs(estadoAtual, entradaAtual);

  //pegamos a saida com fo
  saida = fo(estadoAtual);

  //enviamos a saida na serial
  Serial.print(saida);
  digitalWrite(LED_BUILTIN, saida);
  
  delay(10); // delay pra estabilidade
}

S fs(S estado, int entrada) {
  if(entrada<cSensibilidade){
    return DIA;
  } else {
    return NOITE;
  }
}

int fo(S estado){
  if(estado==NOITE) {
    return 1;
  }

  if(estado==DIA) {
    return 0;
  }
}
