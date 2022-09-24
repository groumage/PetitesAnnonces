package main.gui;
import main.client.Client;
import main.client.ClientState;
import main.protocol.Domain;
import main.server.Annonce;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class gui {

    private Client client;
    private PerspectiveView view;
    private ArrayList<String> highlightedName;
    private String selectMessageBoxDst = null;
    private Domain selectDomain = null;
    private String selectAnnonce;
    private Annonce memoryAnnonce;
    private JPanel panel1;
    private JButton signInButton;
    private JTextField mailField;
    private JTextField usernameField;
    private JButton signUpButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private JList<Domain> domainList;
    private JTextArea contentArea;
    private JPasswordField pwdField;
    private JButton refreshDomainListButton;
    private JButton createAnnonceButton;
    private JButton openChatButton;
    private JButton signOutButton;
    private JButton updateAnnonceButton;
    private JButton removeAnnonceButton;
    private JList<Annonce> annonceList;
    private JComboBox<Domain> domainComboBox;
    private JCheckBox updateViewCheckBox;
    private JTextField titleAnnonceField;
    private JLabel statusLabel;
    private JButton refreshAnnonceListButton;
    private JLabel ownerLabel;
    private JTextField priceField;
    private JList<String> messageBoxList;
    private JButton sendButton;
    private JTextField chatMessage;
    private JTextArea contentMessage;
    private JCheckBox createViewCheckBox;

    public gui(Client client) {
        this.client = client;
        $$$setupUI$$$();
        this.setStatusLabel(ClientState.DISCONNECTED);
        this.changeView(PerspectiveView.DISCONNECTED);
        this.highlightedName = new ArrayList<>();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        this.messageBoxList.setModel(listModel);
        ((DefaultCaret) (this.contentMessage.getCaret())).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        this.messageBoxList.setCellRenderer(new MessageBoxListRenderer());

        this.annonceList.setCellRenderer(new AnnonceListRenderer());
        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.signUp(mailField.getText(), usernameField.getText(), String.valueOf(pwdField.getPassword()));
                    mailField.setText("");
                    usernameField.setText("");
                    pwdField.setText("");
                } catch (IOException | InvalidAlgorithmParameterException | NoSuchPaddingException |
                         IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                         InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        signInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.signIn(mailField.getText(), String.valueOf(pwdField.getPassword()), true);
                    mailField.setText("");
                    usernameField.setText("");
                    pwdField.setText("");
                } catch (IOException | InvalidAlgorithmParameterException | NoSuchPaddingException |
                         IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                         InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        signOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.signOut();
                } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                         IOException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
                client.getGui().changeView(PerspectiveView.CONNECTED);
            }
        });
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.openConnection();
                    client.requestPublicKeyAndIV();
                } catch (IOException | InvalidAlgorithmParameterException | NoSuchPaddingException |
                         IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException |
                         InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.closeConnection();
                } catch (IOException | NoSuchAlgorithmException ex) {
                    throw new RuntimeException(ex);
                }
                changeView(PerspectiveView.DISCONNECTED);
            }
        });
        createViewCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (createViewCheckBox.isSelected())
                    changeView(PerspectiveView.CREATE_ANNONCE);
                else
                    changeView(PerspectiveView.LOGGED);
            }
        });
        messageBoxList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                contentMessage.setText(client.getMessage(messageBoxList.getSelectedValue()));
                highlightedName.remove(messageBoxList.getSelectedValue());
                System.out.println(highlightedName.size());
                messageBoxList.repaint();
                if (messageBoxList.getSelectedIndex() != -1)
                    selectMessageBoxDst = messageBoxList.getSelectedValue();
            }
        });
        createAnnonceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.createAnnonce((Domain) domainComboBox.getSelectedItem(), titleAnnonceField.getText(), contentArea.getText(), Integer.parseInt(priceField.getText()));
                    titleAnnonceField.setText("");
                    contentArea.setText("");
                    priceField.setText("");
                } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                         IOException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        updateViewCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updateViewCheckBox.isSelected()) {
                    memoryAnnonce = annonceList.getSelectedValue();
                    changeView(PerspectiveView.UPDATE_ANNONCE);
                } else {
                    memoryAnnonce = null;
                    //int memSelectDomain = domainList.getSelectedIndex();
                    changeView(PerspectiveView.LOGGED);
                    //if (memSelectDomain != -1)
                    //    domainList.setSelectedIndex(memSelectDomain);
                }
            }
        });
        domainList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && !createViewCheckBox.isSelected() && !updateViewCheckBox.isSelected()) {
                    try {
                        annonceList.clearSelection();
                        domainComboBox.removeAllItems();
                        client.annonceFromDomain(domainList.getSelectedValue());
                        selectDomain = domainList.getSelectedValue();
                    } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                             IOException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
        annonceList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!annonceList.isSelectionEmpty()) {
                    titleAnnonceField.setText(annonceList.getSelectedValue().getTitle());
                    domainComboBox.removeAllItems();
                    domainComboBox.addItem(annonceList.getSelectedValue().getDomain());
                    domainComboBox.setSelectedItem(annonceList.getSelectedValue().getDomain());
                    contentArea.setText(annonceList.getSelectedValue().getContent());
                    priceField.setText(String.valueOf(annonceList.getSelectedValue().getPrice()));
                    ownerLabel.setText(annonceList.getSelectedValue().getOwner());
                    updateViewCheckBox.setEnabled(ownerLabel.getText().equals(client.getName()));
                    selectAnnonce = annonceList.getName();
                    removeAnnonceButton.setEnabled(annonceList.getSelectedValue().getOwner().equals(client.getName()));
                } else {
                    titleAnnonceField.setText("");
                    domainComboBox.removeAllItems();
                    contentArea.setText("");
                    priceField.setText("");
                    ownerLabel.setText("N/A");
                }
            }
        });
        updateAnnonceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.updateAnnonce(titleAnnonceField.getText(), contentArea.getText(), Integer.parseInt(priceField.getText()), memoryAnnonce.getId());
                } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                         IOException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
                titleAnnonceField.setText("");
                contentArea.setText("");
                priceField.setText("");
            }
        });
        removeAnnonceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    client.removeAnnonce(annonceList.getSelectedValue().getId());
                } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                         IOException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public static void main(String[] args) {

        /*
        JFrame frame = new JFrame("App");
        frame.setContentPane(new gui().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
         */
    }

    public void changeView(PerspectiveView view) {
        this.view = view;
        switch (view) {
            case DISCONNECTED -> {
                this.mailField.setText("");
                this.mailField.setEnabled(false);
                this.mailField.setEditable(false);
                this.pwdField.setText("");
                this.pwdField.setEnabled(false);
                this.pwdField.setEditable(false);
                this.usernameField.setText("");
                this.usernameField.setEnabled(false);
                this.usernameField.setEditable(false);
                this.titleAnnonceField.setText("");
                this.titleAnnonceField.setEditable(false);
                this.titleAnnonceField.setEnabled(false);
                this.priceField.setText("");
                this.priceField.setEditable(false);
                this.priceField.setEnabled(false);
                this.chatMessage.setText("");
                this.chatMessage.setEditable(false);
                this.chatMessage.setEnabled(false);

                this.domainList.setModel(new DefaultListModel<>());
                this.domainList.setEnabled(false);
                this.annonceList.setModel(new DefaultListModel<>());
                this.annonceList.setEnabled(false);
                this.messageBoxList.setModel(new DefaultListModel<>());
                this.messageBoxList.setEnabled(false);

                this.domainComboBox.removeAllItems();
                this.domainComboBox.setEnabled(false);
                this.domainComboBox.setEditable(false);

                this.contentArea.setText("");
                this.contentArea.setEditable(false);
                this.contentArea.setEnabled(false);
                this.contentArea.setText("");
                this.contentMessage.setEditable(false);
                this.contentMessage.setEnabled(false);

                this.ownerLabel.setText("N/A");
                this.ownerLabel.setEnabled(false);

                this.refreshDomainListButton.setEnabled(false);
                this.refreshAnnonceListButton.setEnabled(false);
                this.signInButton.setEnabled(false);
                this.signUpButton.setEnabled(false);
                this.signOutButton.setEnabled(false);
                this.connectButton.setEnabled(true);
                this.disconnectButton.setEnabled(false);
                this.createAnnonceButton.setEnabled(false);
                this.updateAnnonceButton.setEnabled(false);
                this.updateViewCheckBox.setEnabled(false);
                this.removeAnnonceButton.setEnabled(false);
                this.sendButton.setEnabled(false);
                this.openChatButton.setEnabled(false);

                this.createViewCheckBox.setEnabled(false);
                this.updateViewCheckBox.setEnabled(false);
            }
            case CONNECTED -> {
                this.mailField.setText("");
                this.mailField.setEnabled(true);
                this.mailField.setEditable(true);
                this.pwdField.setText("");
                this.pwdField.setEnabled(true);
                this.pwdField.setEditable(true);
                this.usernameField.setText("");
                this.usernameField.setEnabled(true);
                this.usernameField.setEditable(true);
                this.titleAnnonceField.setText("");
                this.titleAnnonceField.setEditable(false);
                this.titleAnnonceField.setEnabled(false);
                this.priceField.setText("");
                this.priceField.setEditable(false);
                this.priceField.setEnabled(false);
                this.chatMessage.setText("");
                this.chatMessage.setEditable(false);
                this.chatMessage.setEnabled(false);

                this.domainList.setModel(new DefaultListModel<>());
                this.domainList.setEnabled(false);
                this.annonceList.setModel(new DefaultListModel<>());
                this.annonceList.setEnabled(false);
                this.updateMessageBoxList();
                this.messageBoxList.setEnabled(true);

                this.domainComboBox.removeAllItems();
                this.domainComboBox.setEnabled(false);
                this.domainComboBox.setEditable(false);

                this.contentArea.setText("");
                this.contentArea.setEditable(false);
                this.contentArea.setEnabled(false);
                this.contentMessage.setEditable(false);
                this.contentMessage.setEnabled(true);

                this.ownerLabel.setText("N/A");
                this.ownerLabel.setEnabled(false);

                this.refreshDomainListButton.setEnabled(false);
                this.refreshAnnonceListButton.setEnabled(false);
                this.signInButton.setEnabled(true);
                this.signUpButton.setEnabled(true);
                this.signOutButton.setEnabled(false);
                this.connectButton.setEnabled(false);
                this.disconnectButton.setEnabled(true);
                this.createAnnonceButton.setEnabled(false);
                this.updateAnnonceButton.setEnabled(false);
                this.updateViewCheckBox.setEnabled(false);
                this.removeAnnonceButton.setEnabled(false);
                this.sendButton.setEnabled(false);
                this.openChatButton.setEnabled(false);

                this.createViewCheckBox.setEnabled(false);
                this.updateViewCheckBox.setEnabled(false);
            }
            case LOGGED -> {
                this.mailField.setText(this.client.getMail());
                this.mailField.setEditable(false);
                this.mailField.setEnabled(true);
                this.pwdField.setText("");
                this.pwdField.setEditable(false);
                this.pwdField.setEnabled(false);
                this.usernameField.setText(this.client.getName());
                this.usernameField.setEditable(false);
                this.usernameField.setEnabled(true);
                this.titleAnnonceField.setText("");
                this.titleAnnonceField.setEditable(false);
                this.titleAnnonceField.setEnabled(true);
                this.priceField.setText("");
                this.priceField.setEditable(false);
                this.priceField.setEnabled(true);
                this.chatMessage.setText("");
                this.chatMessage.setEditable(false);
                this.chatMessage.setEnabled(true);

                this.updateDomainList();
                this.domainList.setEnabled(true);
                this.updateAnnonceList();
                this.annonceList.setEnabled(true);
                if (this.selectDomain != null)
                    this.clickOnDomain(selectDomain);
                if (selectAnnonce != null)
                    this.clickOnAnnonce(this.selectAnnonce);
                this.updateMessageBoxList();
                this.messageBoxList.setEnabled(true);

                this.domainComboBox.removeAllItems();
                this.domainComboBox.setEnabled(true);
                this.domainComboBox.setEditable(false);

                this.contentArea.setText("");
                this.contentArea.setEditable(false);
                this.contentArea.setEnabled(true);
                this.contentMessage.setEditable(false);
                this.contentMessage.setEnabled(true);
                if (this.selectMessageBoxDst != null)
                    this.contentMessage.setText(this.client.getMessage(this.selectMessageBoxDst));

                this.ownerLabel.setText("N/A");
                this.ownerLabel.setEnabled(false);

                this.refreshDomainListButton.setEnabled(true);
                this.refreshAnnonceListButton.setEnabled(true);
                this.signInButton.setEnabled(false);
                this.signUpButton.setEnabled(false);
                this.signOutButton.setEnabled(true);
                this.createAnnonceButton.setEnabled(false);
                this.updateAnnonceButton.setEnabled(false);
                this.removeAnnonceButton.setEnabled(false);
                this.connectButton.setEnabled(false);
                this.disconnectButton.setEnabled(false);
                this.openChatButton.setEnabled(false);

                this.updateViewCheckBox.setEnabled(false);
                this.createViewCheckBox.setEnabled(true);
            }
            case CREATE_ANNONCE -> {
                this.mailField.setText(this.client.getMail());
                this.mailField.setEditable(false);
                this.mailField.setEnabled(true);
                this.pwdField.setText("");
                this.pwdField.setEditable(false);
                this.pwdField.setEnabled(false);
                this.usernameField.setText(this.client.getName());
                this.usernameField.setEditable(false);
                this.usernameField.setEnabled(true);
                this.titleAnnonceField.setText("");
                this.titleAnnonceField.setEditable(true);
                this.titleAnnonceField.setEnabled(true);
                this.priceField.setText("");
                this.priceField.setEditable(true);
                this.priceField.setEnabled(true);
                this.chatMessage.setText("");
                this.chatMessage.setEditable(false);
                this.chatMessage.setEnabled(false);

                this.domainList.clearSelection();
                this.domainList.setEnabled(false);
                //this.annonceList.setModel(new DefaultListModel<>());
                this.annonceList.clearSelection();
                this.annonceList.setEnabled(false);
                this.messageBoxList.setEnabled(true);

                this.updateDomainComboBox();
                this.domainComboBox.setEnabled(true);
                this.domainComboBox.setEditable(false);

                this.contentArea.setText("");
                this.contentArea.setEditable(true);
                this.contentArea.setEnabled(true);
                this.contentMessage.setEditable(false);
                this.contentMessage.setEnabled(true);

                this.ownerLabel.setText("Yourself");
                this.ownerLabel.setEnabled(false);

                this.refreshDomainListButton.setEnabled(false);
                this.refreshAnnonceListButton.setEnabled(false);
                this.signInButton.setEnabled(false);
                this.signUpButton.setEnabled(false);
                this.signOutButton.setEnabled(false);
                this.createAnnonceButton.setEnabled(true);
                this.updateAnnonceButton.setEnabled(false);
                this.removeAnnonceButton.setEnabled(false);
                this.connectButton.setEnabled(false);
                this.disconnectButton.setEnabled(false);
                this.openChatButton.setEnabled(false);

                this.updateViewCheckBox.setEnabled(false);
                this.createViewCheckBox.setEnabled(true);
            }
            case UPDATE_ANNONCE -> {
                this.mailField.setText(this.client.getMail());
                this.mailField.setEditable(false);
                this.mailField.setEnabled(true);
                this.pwdField.setText("");
                this.pwdField.setEditable(false);
                this.pwdField.setEnabled(false);
                this.usernameField.setText(this.client.getName());
                this.usernameField.setEditable(false);
                this.usernameField.setEnabled(true);
                this.titleAnnonceField.setText(annonceList.getSelectedValue().getTitle());
                this.titleAnnonceField.setEditable(true);
                this.titleAnnonceField.setEnabled(true);
                this.priceField.setText(String.valueOf(annonceList.getSelectedValue().getPrice()));
                this.priceField.setEditable(true);
                this.priceField.setEnabled(true);
                this.chatMessage.setText("");
                this.chatMessage.setEditable(false);
                this.chatMessage.setEnabled(false);

                this.domainList.clearSelection();
                this.domainList.setEnabled(false);
                //this.annonceList.setModel(new DefaultListModel<>());
                this.annonceList.clearSelection();
                this.annonceList.setEnabled(false);
                this.messageBoxList.setEnabled(true);

                this.updateDomainComboBox();
                this.domainComboBox.setEnabled(true);
                this.domainComboBox.setEditable(false);

                this.contentArea.setEditable(true);
                this.contentArea.setEnabled(true);
                this.contentMessage.setEditable(false);
                this.contentMessage.setEnabled(true);

                this.ownerLabel.setText("Yourself");
                this.ownerLabel.setEnabled(false);

                this.refreshDomainListButton.setEnabled(false);
                this.refreshAnnonceListButton.setEnabled(false);
                this.signInButton.setEnabled(false);
                this.signUpButton.setEnabled(false);
                this.signOutButton.setEnabled(false);
                this.createAnnonceButton.setEnabled(false);
                this.updateAnnonceButton.setEnabled(true);
                this.removeAnnonceButton.setEnabled(false);
                this.connectButton.setEnabled(false);
                this.disconnectButton.setEnabled(false);
                this.openChatButton.setEnabled(false);

                this.updateViewCheckBox.setEnabled(true);
                this.createViewCheckBox.setEnabled(false);
            }
        }
    }

    public void clickSignUp() {
        this.signUpButton.doClick();
    }

    public void setMail(String mail) {
        this.mailField.setText(mail);
    }

    public void setUsername(String username) {
        this.usernameField.setText(username);
    }

    public void setPwd(String pwd) {
        this.pwdField.setText(pwd);
    }

    public JPanel getPane() {
        return this.panel1;
    }

    public void clickSignIn() {
        this.signInButton.doClick();
    }

    public void clickSignOut() {
        this.signOutButton.doClick();
    }

    public void clickCreateAnnonce() {
        this.createAnnonceButton.doClick();
    }

    public void setStatusLabel(ClientState state) {
        this.statusLabel.setText(String.valueOf(state));
    }

    public PerspectiveView getPerspective() {
        return this.view;
    }

    public void clickCreateCheckBox() {
        this.createViewCheckBox.doClick();
    }

    public void clickUpdateCheckBox() {
        this.updateViewCheckBox.doClick();
    }

    public ClientState getStatusLabel() {
        return ClientState.valueOf(this.statusLabel.getText());
    }

    public void clickConnect() {
        this.connectButton.doClick();
    }

    public void clickRemoveAnnonce() {
        this.removeAnnonceButton.doClick();
    }

    public void clickUpdateAnnonce() {
        this.updateAnnonceButton.doClick();
    }

    public void clickDisconnect() {
        this.disconnectButton.doClick();
    }

    public String[] getMessageBoxList() {
        ArrayList<String> listContent = new ArrayList<>();
        for (int i = 0; i < this.messageBoxList.getModel().getSize(); i++)
            listContent.add(this.messageBoxList.getModel().getElementAt(i));
        return listContent.toArray(new String[0]);
    }

    public JList<String> getMessageBox() {
        return this.messageBoxList;
    }

    public Annonce[] getAnnonceList() {
        ArrayList<Annonce> annonceContent = new ArrayList<>();
        for (int i = 0; i < this.annonceList.getModel().getSize(); i++)
            annonceContent.add(this.annonceList.getModel().getElementAt(i));
        return annonceContent.toArray(new Annonce[0]);
    }

    public JList<Annonce> getAnnonceJList() {
        return this.annonceList;
    }

    public Domain[] getDomainList() {
        ArrayList<Domain> listContent = new ArrayList<>();
        for (int i = 0; i < this.domainList.getModel().getSize(); i++)
            listContent.add(this.domainList.getModel().getElementAt(i));
        return listContent.toArray(new Domain[0]);
    }

    public JList<Domain> getDomainJList() {
        return this.domainList;
    }

    public Domain[] getDomainInComboBox() {
        ArrayList<Domain> listContent = new ArrayList<>();
        for (int i = 0; i < this.domainComboBox.getModel().getSize(); i++)
            listContent.add(this.domainComboBox.getModel().getElementAt(i));
        return listContent.toArray(new Domain[0]);
    }

    public JComboBox<Domain> getDomainComboBox() {
        return this.domainComboBox;
    }

    public void setDomainList(Domain[] domains) {
        this.client.setDomainsList(domains);
        this.updateDomainList();
    }

    public void updateDomainList() {
        DefaultListModel<Domain> demoList = new DefaultListModel<>();
        if (this.client.getDomains() != null) {
            for (Domain d : this.client.getDomains())
                demoList.addElement(d);
            this.domainList.setModel(demoList);
        }
    }

    public void updateDomainComboBox() {
        this.domainComboBox.removeAllItems();
        if (this.client.getDomains() != null) {
            for (Domain d : this.client.getDomains())
                this.domainComboBox.addItem(d);
        }
    }

    public void updateAnnonceList() {
        DefaultListModel<Annonce> demoList = new DefaultListModel<>();
        if (this.client.getAnnonces() != null) {
            for (Annonce a : this.client.getAnnonces()) {
                demoList.addElement(a);
            }
        }
        this.annonceList.setModel(demoList);

    }

    public void updateMessageBoxList() {
        DefaultListModel<String> demoList = new DefaultListModel<>();
        if (this.client.getMessages() != null) {
            for (String k : this.client.getMessages().keySet())
                demoList.addElement(k);
            this.messageBoxList.setModel(demoList);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(13, 6, new Insets(0, 0, 0, 0), -1, -1));
        mailField = new JTextField();
        mailField.setEditable(false);
        mailField.setEnabled(true);
        mailField.setText("");
        panel1.add(mailField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Mail address");
        panel1.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Password");
        panel1.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        usernameField = new JTextField();
        panel1.add(usernameField, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Username");
        panel1.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pwdField = new JPasswordField();
        panel1.add(pwdField, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        signOutButton = new JButton();
        signOutButton.setText("Sign out");
        panel1.add(signOutButton, new com.intellij.uiDesigner.core.GridConstraints(3, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        annonceList = new JList();
        annonceList.setEnabled(false);
        panel1.add(annonceList, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 6, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Annonce list");
        panel1.add(label4, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 5, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        connectButton = new JButton();
        connectButton.setText("Connect");
        panel1.add(connectButton, new com.intellij.uiDesigner.core.GridConstraints(4, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        disconnectButton = new JButton();
        disconnectButton.setText("Disconnect");
        panel1.add(disconnectButton, new com.intellij.uiDesigner.core.GridConstraints(5, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        updateViewCheckBox = new JCheckBox();
        updateViewCheckBox.setText("Update view");
        panel1.add(updateViewCheckBox, new com.intellij.uiDesigner.core.GridConstraints(6, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        refreshDomainListButton = new JButton();
        refreshDomainListButton.setText("Refresh list");
        panel1.add(refreshDomainListButton, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        domainList = new JList();
        panel1.add(domainList, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Domain list");
        panel1.add(label5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Status");
        panel1.add(label6, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusLabel = new JLabel();
        statusLabel.setText("Label");
        panel1.add(statusLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Message box");
        panel1.add(label7, new com.intellij.uiDesigner.core.GridConstraints(11, 0, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        messageBoxList = new JList();
        panel1.add(messageBoxList, new com.intellij.uiDesigner.core.GridConstraints(11, 1, 2, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 3, new Insets(5, 5, 5, 5), -1, -1));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 10, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label8 = new JLabel();
        label8.setText("Title");
        panel2.add(label8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        titleAnnonceField = new JTextField();
        panel2.add(titleAnnonceField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        domainComboBox = new JComboBox();
        panel2.add(domainComboBox, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Domain");
        panel2.add(label9, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentArea = new JTextArea();
        panel2.add(contentArea, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Content");
        panel2.add(label10, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        priceField = new JTextField();
        panel2.add(priceField, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Price");
        panel2.add(label11, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Owner");
        panel2.add(label12, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ownerLabel = new JLabel();
        ownerLabel.setText("Owner name");
        ownerLabel.setToolTipText("ownerLabel.getText()");
        panel2.add(ownerLabel, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openChatButton = new JButton();
        openChatButton.setText("Open chat");
        panel2.add(openChatButton, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createAnnonceButton = new JButton();
        createAnnonceButton.setText("Create annonce");
        panel1.add(createAnnonceButton, new com.intellij.uiDesigner.core.GridConstraints(8, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        updateAnnonceButton = new JButton();
        updateAnnonceButton.setText("Update annonce");
        panel1.add(updateAnnonceButton, new com.intellij.uiDesigner.core.GridConstraints(9, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeAnnonceButton = new JButton();
        removeAnnonceButton.setText("Remove annonce");
        panel1.add(removeAnnonceButton, new com.intellij.uiDesigner.core.GridConstraints(10, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        refreshAnnonceListButton = new JButton();
        refreshAnnonceListButton.setText("Refresh list");
        panel1.add(refreshAnnonceListButton, new com.intellij.uiDesigner.core.GridConstraints(10, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        createViewCheckBox = new JCheckBox();
        createViewCheckBox.setText("Create view");
        panel1.add(createViewCheckBox, new com.intellij.uiDesigner.core.GridConstraints(7, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sendButton = new JButton();
        sendButton.setText("Send");
        panel1.add(sendButton, new com.intellij.uiDesigner.core.GridConstraints(12, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chatMessage = new JTextField();
        panel1.add(chatMessage, new com.intellij.uiDesigner.core.GridConstraints(12, 2, 1, 3, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        signUpButton = new JButton();
        signUpButton.setText("Sign up");
        panel1.add(signUpButton, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        signInButton = new JButton();
        signInButton.setText("Sign in");
        panel1.add(signInButton, new com.intellij.uiDesigner.core.GridConstraints(2, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(11, 2, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        contentMessage = new JTextArea();
        contentMessage.setColumns(50);
        contentMessage.setLineWrap(true);
        contentMessage.setRows(8);
        contentMessage.setText("");
        contentMessage.setWrapStyleWord(true);
        contentMessage.putClientProperty("html.disable", Boolean.FALSE);
        scrollPane1.setViewportView(contentMessage);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private static class AnnonceListRenderer extends JLabel implements ListCellRenderer<Annonce> {
        public AnnonceListRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Annonce> list, Annonce value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value.getTitle());
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

    private class MessageBoxListRenderer extends JLabel implements ListCellRenderer<String> {

        public MessageBoxListRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            setText(value);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            if (highlightedName.contains(value))
                setText(value + "(new)");
            return this;
        }
    }

    public void clickOnDomain(Domain d) {
        for (int i = 0; i < this.domainList.getModel().getSize(); i++)
            if (this.domainList.getModel().getElementAt(i) == d) {
                this.domainList.setSelectedIndex(i);
                return;
            }
    }

    public void clickOnAnnonce(String title) {
        for (int i = 0; i < this.annonceList.getModel().getSize(); i++) {
            if (this.annonceList.getModel().getElementAt(i).getTitle().equals(title)) {
                this.annonceList.setSelectedIndex(i);
                return;
            }
        }
    }

    public void highlightDst(String dst) {
        if (!this.highlightedName.contains(dst) && (this.selectMessageBoxDst == null || !this.selectMessageBoxDst.equals(dst)))
            this.highlightedName.add(dst);
        this.messageBoxList.repaint();
        if (selectMessageBoxDst != null)
            if (selectMessageBoxDst.equals(dst))
                this.contentMessage.setText(this.client.getMessage(dst));
    }

    public JTextField getMailField() {
        return this.mailField;
    }

    public JPasswordField getPwdField() {
        return this.pwdField;
    }

    public JTextField getUsernameField() {
        return this.usernameField;
    }

    public JTextField getTitleAnnonceField() {
        return this.titleAnnonceField;
    }

    public JTextField getPriceField() {
        return this.priceField;
    }

    public JTextArea getContentAnnonce() {
        return this.contentArea;
    }

    public JCheckBox getUpdateCheckBox() {
        return this.updateViewCheckBox;
    }

    public JButton getRemoveAnnonceButton() {
        return removeAnnonceButton;
    }
}
