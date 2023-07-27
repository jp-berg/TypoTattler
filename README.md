# Description

TypoTattler disassembles a textfile into its individual words and checks them against the default wordlist (or a dictionary if provided).

# Usage

Requires Java > 17.

`java -jar typotattler.jar FILE [DICTIONARY]`

FILE should be a normal textfile. DICTIONARY should be a textfile consisting of one individual word per line. If no dictionary is provided TypoTattler will look for the files `/usr/share/dict/words` and `/usr/dict/words` to use as the wordlist. If those are not found, the program will use the embedded dictionary ('american-english-huge'). Note that the program may have trouble finding the user dictionaries when being run from inside an IDE.

After the file dissasembly the program walks through the text, mistake by mistake. On each the program stops and provides the user with the following options:

* n - *Display the next valid mistake and (if not already shown) the full line the mistake belongs to.*
* p - *Display the previous valid mistake and (if not already shown) the full line the mistake belongs to.*
* r - *Provide a revision for the current (and optionally all other matching) mistakes.*
* s - *Get a list of words from DICTIONARY that are close to the current mistake. Select a replacement by choosing the corresponding number.*
* a - *Add the mistake to the user DICTIONARY. All future occurrences of the mistake in this FILE (now marked as invalid) and others will not be marked as mistake again.*
* i - *Ignore this and all future occurrences of this mistake in this FILE (marks them as invalid). Resets when reloading the FILE.*
* c - *Print the previous, the current and the next line corresponding to the current mistake.*
* l - *Go to the first valid mistake in the line referenced by the number or in the lines following after.*
* o - *Get a quick overview over the key commands described here.*
* e - *Exit the program and save the FILE if it was modified.*
