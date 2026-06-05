1. Przygotowanie "środowiska"
```java
EmailToRawDataConverter converter = new EmailToRawDataConverter();
GmailMessage msg = new GmailMessage();
```
Tworzysz tutaj obiekt konwertera (narzędzie, które będziemy testować) oraz pusty obiekt wiadomości, którą zaraz wypełnimy danym
2. Wypełnienie danych wejściowych
```java
msg.setMessageId("msg-001");
msg.setSubject("Test Subject");
// ... (i tak dalej)
```
Tutaj "symulujesz" przychodzący e-mail. Wypełniasz go przykładowymi danymi: ID wiadomości, tematem, treścią, nadawcą, datą oraz etykietami. To jest Twoje **wejście** (input).
3. Wykonanie konwersji
```java
RawData data = converter.convert(msg);
```
To najważniejszy moment. Przekazujesz wiadomość do konwertera, a on zwraca gotowy obiekt [[RawData]]. Teraz musimy sprawdzić, czy zrobił to dobrze.
4. Weryfikacja (Asercje)
```java
assertEquals("Gmail", data.getSource());
assertEquals("Test Subject", data.getTitle());
// ...
assertTrue(data.getMetadata().containsKey("labels"));
```
Metody `assertEquals` i `assertTrue` to "strażnicy". Sprawdzają one, czy to, co wypluł konwerter, zgadza się z Twoimi oczekiwaniami:

- Czy źródło to "Gmail"?
    
- Czy tytuł w `RawData` jest taki sam jak temat w [[GmailMessage]]?
    
- Czy etykiety zostały poprawnie przeniesione do metadanych?
    

Jeśli którakolwiek z tych wartości byłaby inna niż oczekiwana, test wyrzuci błąd – dzięki temu wiesz, że coś w kodzie konwertera wymaga naprawy.