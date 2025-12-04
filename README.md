# AppLetras
Repositorio para hacer la aplicación de karaoke.

Tenéis que instalaros git en vuestros ordenadores. Desde este enlace podéis descargaroslo: https://git-scm.com/install/windows

Luego desde el propio terminal de git o desde el vuestro tenéis que configurar vuestro Nombre y vuestro email. Los comandos para windows son los siguientes:

git config --global user.name "Tu Nombre" git config --global user.email "tu.correo@ejemplo.com"

Y luego una pequeña guía con los comandos más comunes:

COMANDOS DE FLUJO DE TRABAJO

git init: Inicializa un nuevo repositorio Git local en el direcotrio actual -> git init

git clone: Crea una copia de un repositorio remoto (en github) en tu ordenador -> git clone <URL_del_repositorio>

git status: Muestra los archivos modificados, los que están listos y los nuevos -> git status

git add: Añade archivos modificados o nuevos al área de preparación (Esto no actualiza el repositorio de github, solo prepara los archivos para enviarlos al repositorio de github) -> git add nombre_archivo.txt o gid add . (el . para añadir todos los archivos)

git commit: Guarda de forma permanente los archivos que estén en el área de preparación -> git commit -m "Mensaje descriptivo del cambio"

git log: Muestra el historial completo de commits del repositorio -> git log (ver completo) o git log --oneline (ver resumido)

git diff: Muestra las diferencias línea por línea entre el área de trabajo y el área de preparación -> git diff

COMANDOS DE SINCRONIZACIÓN

git fetch: Descarga los cambios y las ramas del repositorio remoto sin aplicarlos a tu rama local -> git fetch origin

git pull: Descarga los cambios del repositorio remoto y los fusiona automáticamente con tu rama local actual

git push: Sube tus commits locales al repositorio remoto para que estén disponibles.

COMANDOS PARA DESHACER CAMBIOS

git reset: Deshace cambios moviendo el puntero HEAD. (MUCHO CUIDADO CON ESTO, QUE OS PODÉIS CARGAR COSAS) -> git reset HEAD~1

git revert: Crea un nuevo commit que deshace los cambios de un commit anterior. Es seguro para usar en commits que ya han sido subidos al repositorio remoto. -> git revert <hash_del_commit_a_deshacer>

git stash: Guarda temporalmente los cambios sin commitear. Se suele usar para ramas, así que no es muy aconsejable de usar -> git stash y luego git stash pop para aplicar los cambios

git clean: Elimina archivos que Git no está rastreando -> git clean -n (muestra lo que se borraría) y git clean -f (ejecuta el borrado)