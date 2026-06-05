#### 1. Opakowanie (Pakiet)

`package com.dailytask.core.domain;` To adres tego pliku w projekcie. Dzięki niemu inne części programu wiedzą, gdzie znaleźć tę klasę.

#### 2. Pola (Cechy wiadomości)

To miejsca, w których przechowywane są konkretne dane. Zostały oznaczone jako `private`, co oznacza, że są „zamknięte” wewnątrz klasy i nikt nie może ich zmienić bezpośrednio (to tzw. **enkapsulacja**).

- `messageId`: Unikalny identyfikator maila.
    
- `from`: Nadawca.
    
- `subject`: Temat.
    
- `body`: Treść wiadomości.
    
- `receivedDate`: Data otrzymania.
    
- `labels`: Lista etykiet (np. "Praca", "Ważne").
    
- `isUnread`: Czy wiadomość jest nieprzeczytana.
    

#### 3. Konstruktor

`public GmailMessage() {}` To tzw. konstruktor domyślny. Pozwala na stworzenie „pustej” wiadomości w pamięci programu, którą wypełnisz danymi później (używając metod `set`).

#### 4. Gettery i Settery (Drzwi do danych)

Ponieważ pola są `private`, nie możemy się do nich odwołać bezpośrednio. Używamy do tego metod:

- **Gettery (`get...`)**: Pozwalają odczytać wartość (np. `getMessageId()` zwraca ID).
    
- **Settery (`set...`)**: Pozwalają ustawić lub zmienić wartość (np. `setSubject("Witaj")` zmienia temat maila na "Witaj").
    

#### 5. Metoda `toString()`

`@Override` oznacza, że nadpisujemy standardowe zachowanie Javy. Dzięki tej metodzie, jeśli będziesz chciał wypisać wiadomość w konsoli, zobaczysz czytelny napis (np. `GmailMessage{id='123', subject='Cześć'...}`), zamiast skomplikowanego kodu systemowego.