application {
    title = 'Pardalote'
    startupGroups = ['pardalote']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = false

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "pardalote"
    'pardalote' {
        model      = 'pardalote.PardaloteModel'
        view       = 'pardalote.PardaloteView'
        controller = 'pardalote.PardaloteController'
    }

}
