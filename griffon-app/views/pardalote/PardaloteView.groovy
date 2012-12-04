package pardalote

import ca.odell.glazedlists.*
import ca.odell.glazedlists.gui.*
import ca.odell.glazedlists.swing.*
import java.awt.*
import java.awt.dnd.*
import java.awt.datatransfer.*
import javax.swing.*
import javax.swing.filechooser.*
import net.miginfocom.swing.*

def confirmAndExecute( frame, message, initFocus, action ) {
    { evt ->
        if( JOptionPane.showOptionDialog(
                frame,
                message,
                "確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                [
                    "はい",
                    "いいえ"
                ] as String[],
                initFocus,
            ) == JOptionPane.YES_OPTION ) {
            action(evt)
        }
    }

}

final Color FOCUS_COLOR = new Color(200, 255, 200)

def createTableModel(eventList, Map columns) {
    new EventTableModel(
        eventList,
        [
            getColumnCount: { columns*.key.size() },
            getColumnName: { index -> columns*.key[index] },
            getColumnValue: { object, index -> columns*.value[index](object) },
        ] as TableFormat
    )
}

application(
    id:"mainFrame",
    title:"${model.appName}",
    pack: true,
    show:true,
    defaultCloseOperation:JFrame.DO_NOTHING_ON_CLOSE,
    preferredSize:[500,800],
    windowClosing:controller.windowClosing,
    locationByPlatform:true,
    iconImage: imageIcon('/griffon-icon-48x48.png').image,
    iconImages: [
        imageIcon('/griffon-icon-48x48.png').image,
        imageIcon('/griffon-icon-32x32.png').image,
        imageIcon('/griffon-icon-16x16.png').image
    ],
    enabled:bind( source:model, sourceProperty:"editable", mutual:true ),
) {
    popupMenu(
        id:"editPopup",
    ) {
        menuItem("切り取り(T)", actionPerformed:{ e ->
            e.source.parent.parent.cut()
        })
        menuItem("コピー(C)", actionPerformed:{ e ->
            e.source.parent.parent.copy()
        })
        menuItem("貼り付け(P)", actionPerformed:{ e ->
            e.source.parent.parent.paste()
        })
        menuItem("削除(D)", actionPerformed:{ e ->
            def tf = e.source.parent.parent
            def text = ""

            if ( tf.selectionStart != 0 )
                text += tf.text[0..( tf.selectionStart-1 )]
            if ( tf.selectionEnd != tf.text.size() )
                text += tf.text[( tf.selectionEnd )..-1]
             tf.text = text
        })
    }

    view.editPopup.metaClass.parent = null

    menuBar() {
        menu("設定") {
            menuItem(
                "facebook認証",
                actionPerformed:confirmAndExecute(
                    view.mainFrame,
                    "facebookへのアクセス認証を取得します。\nよろしいですか？",
                    "はい",
                    controller.auth,
                ),
                enabled:bind{model.accessToken.empty}
            )
            menuItem(
                "facebook認証解除",
                actionPerformed:confirmAndExecute(
                    view.mainFrame,
                    "facebookへのアクセス認証を解除します。\nよろしいですか？",
                    "いいえ",
                    controller.unauth,
                ),
                enabled:bind{!model.accessToken.empty}
            )
        }

        menu("設定(記事)") {
            checkBoxMenuItem(
                "自動送信ON",
                selected:bind( source:model, sourceProperty:"doAutoSendStatusMessage", mutual:true ),
                actionPerformed:controller.updateSettings,
            )
            separator()
            checkBoxMenuItem(
                "送信後に送信データを削除する",
                selected:bind( source:model, sourceProperty:"doEraseSendStatusMessage", mutual:true ),
                actionPerformed:controller.updateSettings,
            )
        }
        menu("設定(メッセージ)") {
            checkBoxMenuItem(
                "自動送信ON",
                selected:bind( source:model, sourceProperty:"doAutoSendMessage", mutual:true ),
                actionPerformed:controller.updateSettings,
            )
            separator()
            checkBoxMenuItem(
                "送信後に送信データを削除する",
                selected:bind( source:model, sourceProperty:"doEraseSendMessage", mutual:true ),
                actionPerformed:controller.updateSettings,
            )
        }
        menu("設定(誕生日メッセージ)") {
            checkBoxMenuItem(
                "自動送信ON",
                selected:bind( source:model, sourceProperty:"doAutoSendBirthdayMessage", mutual:true ),
                actionPerformed:controller.updateSettings,
            )
        }
        menu("設定(いいね！)") {
            checkBoxMenuItem(
                "自動クリックON",
                selected:bind('doAutoClickLike', target:model, mutual:true),
                actionPerformed:controller.updateSettings,
            )
        }
    }

    borderLayout()

    panel(
        constraints:BorderLayout.NORTH,
        layout:new MigLayout("fillx", "[][grow]10[][grow]")
    ) {
        label(
            text:"ユーザ名"
        )
        textField(
            text:bind( source:model, sourceProperty:"fullName", mutual:true ),
            enabled:false,
            disabledTextColor:Color.BLACK,
            background:Color.GRAY,
            constraints:"grow",
            dragEnabled:true,
        )
        label(
            text:"メール",
        )
        textField(
            text:bind( source:model, sourceProperty:"email", mutual:true ),
            enabled:false,
            disabledTextColor:Color.BLACK,
            background:Color.GRAY,
            constraints:"grow",
            dragEnabled:true,
        )
    }

    tabbedPane(
        constraints:BorderLayout.CENTER,
        id:"mainTab",
    ) {
        panel(
            name:"記事 管理",
        ) {
            borderLayout()

            panel(
                constraints:BorderLayout.NORTH,
                layout:new MigLayout("", "[right, 80][grow, fill]" , ""),
            ) {

                label(
                    text:"件名" ,
                )
                textField(
                    text:bind( source:model, sourceProperty:"statusMessageSubject", mutual:true ),
                    constraints:"growx, wrap",
                    dragEnabled:true,
                    mousePressed:{ e ->
                        if( SwingUtilities.isRightMouseButton(e) ) {
                            view.editPopup.parent = e.source
                            view.editPopup.show(e.source, e.x, e.y)
                        }
                    },
                )

                label( text:"本文" )
                scrollPane(
                    constraints:"growx, wrap",
                ) {
                    textArea(
                        text:bind( source:model, sourceProperty:"statusMessageBody", mutual:true ),
                        rows:5,
                        dragEnabled:true,
                        keyPressed:{ e ->
                            if(e.keyCode == KeyEvent.VK_TAB) {
                                if(e.modifiers > 0)
                                    e.source.transferFocusBackward()
                                else
                                    e.source.transferFocus()
                                e.consume()
                            }
                        },
                        mousePressed:{ e ->
                            if( SwingUtilities.isRightMouseButton(e) ) {
                                view.editPopup.parent = e.source
                                view.editPopup.show(e.source, e.x, e.y)
                            }
                        },
                    )
                }

                label( text:"フォト" )
                textField(
                    id:"statusMessageImageFilePath",
                    text:bind( source:model, sourceProperty:"statusMessageImageFilePath", mutual:true ),
                    constraints:"growx, split 2",
                    editable:false,
                    background:new Color(188, 245, 197),
                )
                view.'statusMessageImageFilePath'.transferHandler = [
                    importData:{ javax.swing.TransferHandler.TransferSupport support ->
                        if(!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
                            return false
                        }
                        support.transferable.getTransferData(DataFlavor.javaFileListFlavor).findAll{
                            it != null
                        }.each{
                            model.statusMessageImageFilePath = it.toString()
                        }
                        return true
                    },
                    canImport:{ javax.swing.TransferHandler.TransferSupport support ->
                        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    }
                ] as TransferHandler
                button(
                    text:"選択",
                    constraints:"wrap",
                    actionPerformed:{ evt ->
                        fileChooser(
                            id:"fc",
                            fileSelectionMode:JFileChooser.FILES_ONLY,
                        )
                        view.fc.with{
                            addChoosableFileFilter new FileNameExtensionFilter(
                                "*.jpg", "jpg", "jpeg",
                            )
                            addChoosableFileFilter new FileNameExtensionFilter(
                                "*.png", "png",
                            )
                            addChoosableFileFilter new FileNameExtensionFilter(
                                "*.gif", "gif",
                            )
                        }

                        if(fc.showOpenDialog() == JFileChooser.APPROVE_OPTION) {
                            model.statusMessageImageFilePath = fc.selectedFile.absolutePath
                        }
                    }
                )

                label( text:"送信時間帯" )
                comboBox(
                    items:[ "" ] + (1..8).collect{ i -> ((i-1)*3).toString().padLeft(2, "0") + ":00 〜 " + (i*3-1).toString().padLeft(2, "0") + ":59" },
                    constraints:"split 2",
                    selectedIndex:bind( source:model, sourceProperty:"statusMessageSendPredeterminedTime", mutual:true ),
                    actionPerformed:{ evt ->
                        if ( model.statusMessageSendPredeterminedTime != 0 ) {
                            model.statusMessageSendWaitingHours = 0
                        }
                    },
                )

                label( text:"送信待ち時間" )
                comboBox(
                    items:[ "" ] + (1..8).collect{ i -> (i)*3 + "時間後" },
                    constraints:"wrap",
                    selectedIndex:bind( source:model, sourceProperty:"statusMessageSendWaitingHours", mutual:true ),
                    actionPerformed:{ evt ->
                        if ( model.statusMessageSendWaitingHours != 0 ) {
                            model.statusMessageSendPredeterminedTime = 0
                        }
                    },
                )

                panel(
                    constraints:"growx, span",
                ) {
                    button(
                        text:"取消",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "入力された内容をクリアします。\nよろしいですか？",
                            "いいえ",
                            controller.clearStatusMessage,
                        ),
                        enabled:bind{
                            def subject = model.statusMessageSubject
                            def body = model.statusMessageBody
                            def sendPredeterminedTime = model.statusMessageSendPredeterminedTime
                            def imageFilePath = model.statusMessageImageFilePath
                            def sendWaitingHours = model.statusMessageSendWaitingHours
                            ( subject.size() > 0 || body.size() > 0 ||
                            imageFilePath.size() > 0 ||
                            sendPredeterminedTime != 0 || sendWaitingHours != 0 )
                        },
                    )
                    button(
                        text:"登録",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "記事を保存します。\nよろしいですか？",
                            "はい",
                            controller.insertStatusMessage,
                        ),
                        visible:bind{model.statusMessageId.empty},
                        enabled:bind{
                            def subject = model.statusMessageSubject
                            def body = model.statusMessageBody
                            def sendPredeterminedTime = model.statusMessageSendPredeterminedTime
                            def sendWaitingHours = model.statusMessageSendWaitingHours
                            model.countOfNotYetSendStatusMessageIsNotOverMax &&
                            subject.size() > 0 && body.size() > 0 &&
                            (sendPredeterminedTime != 0 || sendWaitingHours != 0)
                        },
                    )
                    button(
                        text:"変更",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "記事を保存します。\nよろしいですか？",
                            "はい",
                            controller.updateStatusMessage,
                        ),
                        visible:bind{!model.statusMessageId.empty},
                        enabled:bind{
                            def subject = model.statusMessageSubject
                            def body = model.statusMessageBody
                            def sendPredeterminedTime = model.statusMessageSendPredeterminedTime
                            def imageFilePath = model.statusMessageImageFilePath
                            def sendWaitingHours = model.statusMessageSendWaitingHours

                            return !model.statusMessageId.empty && (
                                subject != model.snapshot.statusMessageSubject || body != model.snapshot.statusMessageBody ||
                                imageFilePath != model.snapshot.statusMessageImageFilePath ||
                                sendPredeterminedTime != model.snapshot.statusMessageSendPredeterminedTime || sendWaitingHours != model.snapshot.statusMessageSendWaitingHours
                            )
                        },
                    )
                    button(
                        text:"すぐに投稿",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "記事を保存し、直ちに投稿を開始します。\nよろしいですか？",
                            "はい",
                            controller.insertCurrentStatusMessage,
                        ),
                        enabled:bind{
                            def subject = model.statusMessageSubject
                            def body = model.statusMessageBody
                            model.countOfNotYetSendStatusMessageIsNotOverMax &&
                            subject.size() > 0 && body.size() > 0
                        },
                    )
                    button(
                        text:"削除",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "本当に記事を削除しますか？",
                            "いいえ",
                            controller.deleteStatusMessage,
                        ),
                        visible:bind{!model.statusMessageId.empty},
                    )
                }
            }

            scrollPane(
                constraints:BorderLayout.CENTER,
            ) {
                table(
                    selectionMode:ListSelectionModel.SINGLE_SELECTION,
                    autoCreateRowSorter:true,
                    model:createTableModel(
                        model.statusMessageEventList,
                        [
                            "件名":{ it.subject },
                            "更新日時":{
                                def updatedTime = Calendar.instance
                                updatedTime.timeInMillis = it.updatedTime
                                updatedTime.format("yy/MM/dd HH:mm")
                            },
                            "送信日時":{
                                if( it.sendTime ) {
                                    def sendTime = Calendar.instance
                                    sendTime.timeInMillis = it.sendTime
                                    sendTime.format("yy/MM/dd HH:mm")
                                } else {
                                    ""
                                }
                            },
                        ]
                    ),
                    mouseClicked:controller.selectStatusMessage
                ) {
                    current.tableHeader.reorderingAllowed = false
                }
            }

        }


        panel(
            name:"メッセージ 管理",
        ) {
            borderLayout()

            panel(
                constraints:BorderLayout.NORTH,
                layout:new MigLayout("", "[right, 80][grow, fill]" , "")
            ) {

                label(
                    text:"宛先" ,
                )
                scrollPane(
                    constraints:"growx, split 2",
                ) {
                    textArea(
                        text:bind( source:model, sourceProperty:"messageToFriendsName", mutual:true ),
                        rows:5,
                        dragEnabled:true,
                        enabled:false,
                        disabledTextColor:Color.BLACK,
                    )
                }
                button(
                    text:"友人を選択",
                    constraints:"wrap",
                    actionPerformed:{ evt ->
                        if( model.friendsEventList.empty ) {
                            controller.reflushFriends(evt)
                        }
                        model.dialogCaller = "message"
                        view.messageToFriendsSelectedDialog.show()
                    }
                )

                dialog(
                    owner:view.mainFrame,
                    id:"messageToFriendsSelectedDialog",
                    size:[350,650],
                    modal:true,
                ) {

                    borderLayout()

                    panel(
                            constraints:BorderLayout.CENTER,
                         ) {

                        borderLayout()

                        panel(
                            constraints:BorderLayout.NORTH,
                        ) {

                            migLayout(
                                layoutConstraints:"fillx",
                                columnConstraints:"[grow][grow]",
                                rowConstraints:"[]",
                            )

                            label(
                                text:"名前",
                            )
                            textField(
                                text:bind( source:model, sourceProperty:"dialogConditionOfUserName", mutual:true ),
                                columns:6,
                                dragEnabled:true,
                                constraints:"growx, span 2",
                            )
                            button(
                                text:"検索",
                                actionPerformed:controller.reflushFriends,
                                enabled:bind{ model.editing },
                                constraints:"",
                            )

                        }

                        scrollPane(
                            constraints:BorderLayout.CENTER,
                        ) {
                            list(
                                id:"friendsListList",
                                selectionMode:ListSelectionModel.MULTIPLE_INTERVAL_SELECTION ,
    //                            model:new EventListModel(model.friendsEventList.collect {
    //                                """<html><center><img src="${it.picture}"><strong>${it.name}</strong></center></html>"""
    //                            } as [],
                                model:new EventListModel(model.friendsEventList),
                                cellRenderer:{ JList list, Object value, int index, boolean isSelected, boolean cellHasFocus ->
                                    label(
                                        text:"${value.name}",
                                        icon:value.picture?.data?.url ? imageIcon(
                                            url:new URL(value.picture?.data?.url)
                                        ) : null,
                                        opaque:true,
                                        background:isSelected ? list.getSelectionBackground(): list.getBackground(),
                                        foreground:isSelected ? list.getSelectionForeground(): list.getForeground(),
                                    )
                                } as ListCellRenderer,
                                doubleBuffered:true,
                            )
                        }

                        panel(
                            constraints:BorderLayout.SOUTH,
                            layout:new MigLayout("fillx", "[right,grow,fill]" , "")
                        ) {
                            button(
                                text:"選択した友人で決定",
                                actionPerformed:controller.selectMessageToFriends,
                            )
                        }

                    }

                    textField(
                        constraints:BorderLayout.SOUTH,
                        text:bind( source:model,  sourceProperty:"dialogStatusbarText"  ),
                        enabled:false,
                        disabledTextColor:Color.BLUE,
                        background:Color.GRAY,
                        font:new Font("monospace", Font.BOLD, 13),
                    )
                }

                view.messageToFriendsSelectedDialog.hide()


                label(
                    text:"件名" ,
                )
                textField(
                    text:bind( source:model, sourceProperty:"messageSubject", mutual:true ),
                    constraints:"growx, wrap",
                    dragEnabled:true,
                    mousePressed:{ e ->
                        if( SwingUtilities.isRightMouseButton(e) ) {
                            view.editPopup.parent = e.source
                            view.editPopup.show(e.source, e.x, e.y)
                        }
                    },
                )

                label( text:"本文" )
                scrollPane(
                    constraints:"growx, wrap",
                ) {
                    textArea(
                        text:bind( source:model, sourceProperty:"messageBody", mutual:true ),
                        rows:5,
                        dragEnabled:true,
                        keyPressed:{ e ->
                            if(e.keyCode == KeyEvent.VK_TAB) {
                                if(e.modifiers > 0)
                                    e.source.transferFocusBackward()
                                else
                                    e.source.transferFocus()
                                e.consume()
                            }
                        },
                        mousePressed:{ e ->
                            if( SwingUtilities.isRightMouseButton(e) ) {
                                view.editPopup.parent = e.source
                                view.editPopup.show(e.source, e.x, e.y)
                            }
                        },
                    )
                }

                label( text:"フォト" )
                textField(
                    id:"messageImageFilePath",
                    text:bind( source:model, sourceProperty:"messageImageFilePath", mutual:true ),
                    constraints:"growx, split 2",
                    editable:false,
                    background:new Color(188, 245, 197),
                )
                view.'messageImageFilePath'.transferHandler = [
                    importData:{ javax.swing.TransferHandler.TransferSupport support ->
                        if(!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
                            return false
                        }
                        def path = support.transferable.getTransferData(DataFlavor.javaFileListFlavor).findAll{
                            it != null
                        }.each{
                            model.messageImageFilePath  = it.toString()
                        }
                        return true
                    },
                    canImport:{ javax.swing.TransferHandler.TransferSupport support ->
                        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    }
                ] as TransferHandler
                button(
                    text:"選択",
                    constraints:"wrap",
                    actionPerformed:{ evt ->
                        fileChooser(
                            id:"fc",
                            fileSelectionMode:JFileChooser.FILES_ONLY,
                        )
                        view.fc.with{
                            addChoosableFileFilter new FileNameExtensionFilter(
                                "*.jpg", "jpg", "jpeg",
                            )
                            addChoosableFileFilter new FileNameExtensionFilter(
                                "*.png", "png",
                            )
                            addChoosableFileFilter new FileNameExtensionFilter(
                                "*.gif", "gif",
                            )
                        }

                        if(fc.showOpenDialog() == JFileChooser.APPROVE_OPTION) {
                            model.messageImageFilePath = fc.selectedFile.absolutePath
                        }
                    }
                )

                label( text:"送信時間帯" )
                comboBox(
                    items:[ "" ] + (1..8).collect{ i -> ((i-1)*3).toString().padLeft(2, "0") + ":00 〜 " + (i*3-1).toString().padLeft(2, "0") + ":59" },
                    constraints:"split 2",
                    selectedIndex:bind( source:model, sourceProperty:"messageSendPredeterminedTime", mutual:true ),
                    actionPerformed:{ evt ->
                        if ( model.messageSendPredeterminedTime != 0 ) {
                            model.messageSendWaitingHours = 0
                        }
                    },
                )
                label( text:"送信待ち時間" )
                comboBox(
                    items:[ "" ] + (1..8).collect{ i -> (i)*3 + "時間後" },
                    constraints:"wrap",
                    selectedIndex:bind( source:model, sourceProperty:"messageSendWaitingHours", mutual:true ),
                    actionPerformed:{ evt ->
                        if ( model.messageSendWaitingHours != 0 ) {
                            model.messageSendPredeterminedTime = 0
                        }
                    },
                )

                panel(
                    constraints:"growx, span",
                ) {
                    button(
                        text:"取消",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "入力された内容を取消します。\nよろしいですか？",
                            "いいえ",
                            controller.clearMessage,
                        ),
                        enabled:bind{
                            def toFriends = model.messageToFriends
                            def subject = model.messageSubject
                            def body = model.messageBody
                            def imageFilePath  = model.messageImageFilePath
                            def sendPredeterminedTime = model.messageSendPredeterminedTime
                            def sendWaitingHours = model.messageSendWaitingHours

                            ( toFriends.size() > 0 || subject.size() > 0 || body.size() > 0 ||
                            imageFilePath.size() > 0 ||
                            sendPredeterminedTime != 0 || sendWaitingHours != 0 )
                        },
                    )
                    button(
                        text:"登録",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "メッセージを保存します。\nよろしいですか？",
                            "はい",
                            controller.insertMessage,
                        ),
                        visible:bind{model.messageId.empty},
                        enabled:bind{
                            def toFriends = model.messageToFriends
                            def subject = model.messageSubject
                            def body = model.messageBody
                            def sendPredeterminedTime = model.messageSendPredeterminedTime
                            def sendWaitingHours = model.messageSendWaitingHours
                            model.countOfNotYetSendMessageIsNotOverMax &&
                            toFriends.size() > 0 && subject.size() > 0 && body.size() > 0 &&
                            (sendPredeterminedTime != 0 || sendWaitingHours != 0)
                        },
                    )
                    button(
                        text:"修正",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "メッセージを保存します。\nよろしいですか？",
                            "はい",
                            controller.updateMessage,
                        ),
                        visible:bind{!model.messageId.empty},
                        enabled:bind{
                            def toFriends = model.messageToFriends
                            def subject = model.messageSubject
                            def body = model.messageBody
                            def imageFilePath  = model.messageImageFilePath
                            def sendPredeterminedTime = model.messageSendPredeterminedTime
                            def sendWaitingHours = model.messageSendWaitingHours

                            return !model.messageId.empty && (
                                subject != model.snapshot.messageSubject ||
                                body != model.snapshot.messageBody ||
                                imageFilePath != model.snapshot.messageImageFilePath ||
                                sendPredeterminedTime != model.snapshot.messageSendPredeterminedTime || sendWaitingHours != model.snapshot.messageSendWaitingHours
                            )
                        },
                    )
                    button(
                        text:"すぐに投稿",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "メッセージを保存し、直ちに投稿を開始します。\nよろしいですか？",
                            "はい",
                            controller.insertCurrentMessage,
                        ),
                        enabled:bind{
                            def toFriends = model.messageToFriends
                            def subject = model.messageSubject
                            def body = model.messageBody
                            model.countOfNotYetSendMessageIsNotOverMax &&
                            toFriends.size() > 0 && subject.size() > 0 && body.size() > 0
                        },
                    )
                    button(
                        text:"削除",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "本当にメッセージを削除しますか？",
                            "いいえ",
                            controller.deleteMessage,
                        ),
                        visible:bind{!model.messageId.empty}
                    )
                }
            }

            scrollPane(
                constraints:BorderLayout.CENTER,
            ) {
                table(
                    selectionMode:ListSelectionModel.SINGLE_SELECTION,
                    autoCreateRowSorter:true,
                    model:createTableModel(
                        model.messageEventList,
                        [
                            "件名":{ it.subject },
                            "更新日時":{
                                def updatedTime = Calendar.instance
                                updatedTime.timeInMillis = it.updatedTime
                                updatedTime.format("yy/MM/dd HH:mm")
                            },
                            "送信日時":{
                                if( it.sendTime ) {
                                    def sendTime = Calendar.instance
                                    sendTime.timeInMillis = it.sendTime
                                    sendTime.format("yy/MM/dd HH:mm")
                                } else {
                                    ""
                                }
                            },
                        ]
                    ),
                    mouseClicked:controller.selectMessage
                ) {
                    current.tableHeader.reorderingAllowed = false
                }
            }

        }

        panel(
            name:"誕生日メッセージ 管理",
        ) {
            borderLayout()

            panel(
                constraints:BorderLayout.NORTH,
                layout:new MigLayout("", "[right, 80][grow, fill]" , "")
            ) {
                tabbedPane(
                    constraints:"growx, span, wrap",
                ) {
                    scrollPane(
                        name:"誕生日ﾒｯｾｰｼﾞ1",
                    ) {
                        textArea(
                            text:bind( source:model, sourceProperty:"birthdayMessageBody1", mutual:true ),
                            rows:5,
                            dragEnabled:true,
                            keyPressed:{ e ->
                                if(e.keyCode == KeyEvent.VK_TAB) {
                                    if(e.modifiers > 0)
                                        e.source.transferFocusBackward()
                                    else
                                        e.source.transferFocus()
                                    e.consume()
                                }
                            },
                            mousePressed:{ e ->
                                if( SwingUtilities.isRightMouseButton(e) ) {
                                    view.editPopup.parent = e.source
                                    view.editPopup.show(e.source, e.x, e.y)
                                }
                            },
                        )
                    }

                    scrollPane(
                        name:"ﾒｯｾｰｼﾞ2",
                    ) {
                        textArea(
                            text:bind( source:model, sourceProperty:"birthdayMessageBody2", mutual:true ),
                            rows:5,
                            dragEnabled:true,
                            keyPressed:{ e ->
                                if(e.keyCode == KeyEvent.VK_TAB) {
                                    if(e.modifiers > 0)
                                        e.source.transferFocusBackward()
                                    else
                                        e.source.transferFocus()
                                    e.consume()
                                }
                            },
                            mousePressed:{ e ->
                                if( SwingUtilities.isRightMouseButton(e) ) {
                                    view.editPopup.parent = e.source
                                    view.editPopup.show(e.source, e.x, e.y)
                                }
                            },
                        )
                    }

                    scrollPane(
                        name:"ﾒｯｾｰｼﾞ3",
                    ) {
                        textArea(
                            text:bind( source:model, sourceProperty:"birthdayMessageBody3", mutual:true ),
                            rows:5,
                            dragEnabled:true,
                            keyPressed:{ e ->
                                if(e.keyCode == KeyEvent.VK_TAB) {
                                    if(e.modifiers > 0)
                                        e.source.transferFocusBackward()
                                    else
                                        e.source.transferFocus()
                                    e.consume()
                                }
                            },
                            mousePressed:{ e ->
                                if( SwingUtilities.isRightMouseButton(e) ) {
                                    view.editPopup.parent = e.source
                                    view.editPopup.show(e.source, e.x, e.y)
                                }
                            },
                        )
                    }

                    scrollPane(
                        name:"ﾒｯｾｰｼﾞ4",
                    ) {
                        textArea(
                            text:bind( source:model, sourceProperty:"birthdayMessageBody4", mutual:true ),
                            rows:5,
                            dragEnabled:true,
                            keyPressed:{ e ->
                                if(e.keyCode == KeyEvent.VK_TAB) {
                                    if(e.modifiers > 0)
                                        e.source.transferFocusBackward()
                                    else
                                        e.source.transferFocus()
                                    e.consume()
                                }
                            },
                            mousePressed:{ e ->
                                if( SwingUtilities.isRightMouseButton(e) ) {
                                    view.editPopup.parent = e.source
                                    view.editPopup.show(e.source, e.x, e.y)
                                }
                            },
                        )
                    }

                    scrollPane(
                        name:"ﾒｯｾｰｼﾞ5",
                    ) {
                        textArea(
                            text:bind( source:model, sourceProperty:"birthdayMessageBody5", mutual:true ),
                            rows:5,
                            dragEnabled:true,
                            keyPressed:{ e ->
                                if(e.keyCode == KeyEvent.VK_TAB) {
                                    if(e.modifiers > 0)
                                        e.source.transferFocusBackward()
                                    else
                                        e.source.transferFocus()
                                    e.consume()
                                }
                            },
                            mousePressed:{ e ->
                                if( SwingUtilities.isRightMouseButton(e) ) {
                                    view.editPopup.parent = e.source
                                    view.editPopup.show(e.source, e.x, e.y)
                                }
                            },
                        )
                    }

                }

                panel(
                    constraints:"growx, span, wrap",
                ) {
                    borderLayout()

                    button(
                        text:"修正",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "誕生日メッセージテンプレートを保存します。\nよろしいですか？",
                            "はい",
                            controller.updateBirthdayMessageTemplate,
                        ),
                        constraints:BorderLayout.EAST,
                    )
                }


                label(
                    text:"宛先" ,
                )
                scrollPane(
                    constraints:"growx, split 2",
                ) {
                    textArea(
                        text:bind( source:model, sourceProperty:"birthdayMessageToFriendsName", mutual:true ),
                        rows:5,
                        dragEnabled:true,
                        enabled:false,
                        disabledTextColor:Color.BLACK,
                    )
                }
                button(
                    text:"友人を選択",
                    constraints:"wrap",
                    actionPerformed:{ evt ->
                        if( model.friendsEventList.empty ) {
                            controller.reflushFriends(evt)
                        }
                        model.dialogCaller = "birthdayMessage"
                        view.messageToFriendsSelectedDialog.show()
                    }
                )


                label( text:"送信時間帯" )
                comboBox(
                    items:(0..23).collect{ i -> i.toString().padLeft(2, "0") + ":00 〜 " + i.toString().padLeft(2, "0") + ":59" },
                    constraints:"split 2",
                    selectedIndex:bind( source:model, sourceProperty:"birthdayMessageSendPredeterminedTime", mutual:true ),
                )
                panel(
                    constraints:"growx, span",
                ) {
                    button(
                        text:"取消",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "入力された内容を取消します。\nよろしいですか？",
                            "いいえ",
                            controller.clearBirthdayMessage,
                        ),
                    )
                    button(
                        text:"登録",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "誕生日メッセージ送信設定を保存します。\nよろしいですか？",
                            "はい",
                            controller.insertBirthdayMessage,
                        ),
                        visible:bind{model.birthdayMessageId.empty},
                    )
                    button(
                        text:"修正",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "誕生日メッセージ送信設定を保存します。\nよろしいですか？",
                            "はい",
                            controller.updateBirthdayMessage,
                        ),
                        visible:bind{!model.birthdayMessageId.empty},
                    )
                    button(
                        text:"削除",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "本当に誕生日メッセージ送信設定を削除しますか？",
                            "いいえ",
                            controller.deleteBirthdayMessage,
                        ),
                        visible:bind{!model.birthdayMessageId.empty}
                    )
                }


            }

            scrollPane(
                constraints:BorderLayout.CENTER,
            ) {
                table(
                    selectionMode:ListSelectionModel.SINGLE_SELECTION,
                    autoCreateRowSorter:true,
                    model:createTableModel(
                        model.birthdayMessageEventList,
                        [
                            "名前":{ it.toFriendsName },
                            "更新日時":{
                                def updatedTime = Calendar.instance
                                updatedTime.timeInMillis = it.updatedTime
                                updatedTime.format("yy/MM/dd HH:mm")
                            },
//                            "送信日時":{
//                                if( it.sendTime ) {
//                                    def sendTime = Calendar.instance
//                                    sendTime.timeInMillis = it.sendTime
//                                    sendTime.format("yy/MM/dd HH:mm")
//                                } else {
//                                    ""
//                                }
//                            },
                        ]
                    ),
                    mouseClicked:controller.selectBirthdayMessage
                ) {
                    current.tableHeader.reorderingAllowed = false
                }
            }
        }

        panel(
            name:"いいね！ 管理",
        ) {
            borderLayout()

            panel(
                constraints:BorderLayout.NORTH,
            ) {
                migLayout(
                    layoutConstraints:"fillx",
                    columnConstraints:"[grow][grow]",
                    rowConstraints:"[][][][::110]",
                )

                label(
                    text:"【自動いいね！対象の検索条件】" ,
                    foreground:new Color(32, 57, 168),
                    constraints:"grow, span 2, wrap",
                )

                label(
                    text:"投稿時間" ,
                    constraints:"grow",
                )
//                textField(
//                    text:bind( source:model, sourceProperty:"autoClickLikeHoursOfStartFromNow", mutual:true ),
//                    columns:2,
//                    dragEnabled:true,
//                    enabled:bind{ model.editing },
//                    constraints:"growx, split 4",
//                )
                spinner(
                    value:bind( source:model, sourceProperty:"autoClickLikeHoursOfStartFromNow", mutual:true ),
                    'model':spinnerNumberModel(
                        maximum:99,
                        minimum:0,
                    ),
                    constraints:"split 4",

                )
                label(
                    text:"〜" ,
                )
                spinner(
                    value:bind( source:model, sourceProperty:"autoClickLikeHoursOfEndFromNow", mutual:true ),
                    'model':spinnerNumberModel(
                        maximum:99,
                        minimum:0,
                    ),

                )
                label(
                    text:"時間前" ,
                    constraints:"wrap",
                )
                
                label(
                    text:"自分の投稿" ,
                    constraints:"grow",
                )
                comboBox(
                    items:[ "しない", "する" ].collect {
                        "対象と" + it
                    },
                    selectedIndex:bind( source:model, sourceProperty:"autoClickLikeToMe", mutual:true ),
                    actionPerformed:{ evt ->
                        if ( model.statusMessageSendPredeterminedTime != 0 ) {
                            model.statusMessageSendWaitingHours = 0
                        }
                    },
                    constraints:"wrap",
                )

                panel(
                    constraints:"growx, span 2, wrap",
                ) {
                    button(
                        text:"取消",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "入力された内容を取消します。\nよろしいですか？",
                            "いいえ",
                            controller.clearAutoClickLikeSettings,
                        ),
                    )
                    button(
                        text:"修正",
                        actionPerformed:confirmAndExecute(
                            view.mainFrame,
                            "いいね！クリック設定を保存します。\nよろしいですか？",
                            "はい",
                            controller.updateAutoClickLikeSettings,
                        ),
                    )
                }

            }
        }

    }

    textField(
        constraints:BorderLayout.SOUTH,
        text:bind( source:model,  sourceProperty:"statusbarText" ),
        enabled:false,
        disabledTextColor:Color.RED,
        background:Color.GRAY,
        font:new Font("monospace", Font.BOLD, 13),
    )
}

view.mainTab.setTabComponentAt 0, label(
    text:"記事 管理",
    foreground:new Color(32, 57, 168),
    background:Color.BLACK,
    icon:bind{ model.doAutoSendStatusMessage ? silkIcon("time_go") : silkIcon("time_delete") },
)

view.mainTab.setTabComponentAt 1, label(
    text:"メッセージ 管理",
    foreground:new Color(32, 57, 168),
    background:Color.BLACK,
    icon:bind{ model.doAutoSendMessage ? silkIcon("time_go") : silkIcon("time_delete") },
)

view.mainTab.setTabComponentAt 2, label(
    text:"誕生日メッセージ 管理",
    foreground:new Color(32, 57, 168),
    background:Color.BLACK,
    icon:bind{ model.doAutoSendBirthdayMessage ? silkIcon("time_go") : silkIcon("time_delete") },
)

view.mainTab.setTabComponentAt 3, label(
    text:"いいね！ 管理",
    foreground:new Color(32, 57, 168),
    background:Color.BLACK,
    icon:bind{ model.doAutoClickLikeNow ? silkIcon("time_go") : silkIcon("time_delete") },
)

