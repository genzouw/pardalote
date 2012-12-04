log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c#%m%n')
        rollingFile name: 'file',
            maxFileSize:1048576,
            maxBackupIndex:5,
            file: "./pardalote.log",
            layout: pattern(conversionPattern: '%d [%t] %-5p %c#%m%n')
    }

    error  'org.codehaus.griffon'

    info   'griffon.util',
           'griffon.core',
           'griffon.swing',
           'griffon.app'
}

