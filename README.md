# LiveCoached

Vous trouverez ici l'application côté montre de la premiere implementtation du projet de mon stage de fin d'étude.

Veuillez modifier le champs "SERVER_IP" dans "ClientTask" et mettre l'addresse de votre server à la place.

Cette application est à utiliser avec l'application [LiveCoaching](https://github.com/AlexGreau/LiveCoaching) pour tablettes Android. Cette paire d'application sert à conduire une expérience cherchant à prouver nos hypothèses sur de nouvelles façons d'interagir en contexte actif. Voici le schéma avec captures d'écran de la logique de la paire d'applications :

![flowNormal](https://github.com/AlexGreau/LiveCoached/blob/master/readmeImages/flowNormal.PNG)

## Starting Activity

La startingActivity de la montre sert à envoyer le signal "Je suis prêt" à la tablette et recevoir son accord pour continuer.
Elle décodera l'interaction que la tablette réclame, puis lancera la MainActivity avec les paramètres adéquats.

## Main Activity

La mainActivity est lancée par la starting activity suite à l'ordre reçu par la tablette.
Son interface change en fonction de la technique d'interaction demandée.
Son rôle est de détecter l'orientation et la loalisation de la montre pour calculer la correction necesaire pour être sur le bon chemin via les fonctions `public void checkDistance()` et `public void checkAngle()`. La fonciton ` public void vibrate(int pat)` se charge ensuite du feedback haptique si besoin, en faisant appel à la fonction `private void setVibroValues(int style)`.

## Localisation
La localisation de la montre se fait via un "FusedLocationProviderClient" qui formule une "LocationRequest" toutes le 2 secondes. Une fois reçue il déclanche une "LocationCallback" comme décris dans [cette page](https://developer.android.com/training/location/retrieve-current).

## Communications

Voici le schema des communications du système :
![schemaComm](https://github.com/AlexGreau/LiveCoached/blob/master/readmeImages/schemaNormal.PNG)

Dès qu'elle voudra envoyer un ordre, la montre créera une "ClientTask", une classe qui hérite de "AsyncTask" qui gèrera l'envoi des données en arrière plan sans perturber le UiThread.

Pour le décodage des données, une interface "Decoder" contenant la fonction `public void decodeResponse(String rep)` permet différentes interprétations selon l'activity ayant reçu l'ordre.
Cette interface permet de donner en paramètre un "Decoder" aux ClientTask, sans avoir a préciser l'activité en particulier.