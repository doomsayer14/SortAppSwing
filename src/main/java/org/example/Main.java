package org.example;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Main extends JFrame {

    private static final String INTRO_LAYOUT_NAME = "Intro";
    private static final String APPLICATION_NAME = "Sort Application";
    private static final String SORT_BUTTON_NAME = "Sort";
    private static final String SORT_DESC_BUTTON_NAME = "Sort ↓";
    private static final String SORT_ASC_BUTTON_NAME = "Sort ↑";
    private static final String RESET_BUTTON_NAME = "Reset";
    private static final String ENTER_BUTTON_NAME = "Enter";

    private static final int MAX_NUMBER = 1000;
    private static final int MIN_VALUE = 30;
    private static final int ROWS_PER_COLUMN = 10;
    private static final Dimension NUMBER_BUTTON_SIZE = new Dimension(60, 30);
    private static final Dimension MAIN_WINDOW_SIZE = new Dimension(700, 500);
    private static final Color COLOR_BLUE = new Color(0x007BFF);
    private static final Color COLOR_GREEN = new Color(0x28A745);

    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    private final JTextField inputField;
    private final JButton enterButton;

    private final JPanel numbersPanel;
    private final JButton sortButton;
    private final JButton resetButton;

    private final Random random;
    private boolean descending;
    private final List<Integer> numbers;
    private Timer sortTimer;
    private List<int[]> quicksortSteps;

    private final Set<Integer> highlightedIndices;
    private List<Set<Integer>> highlightSteps;

    public Main() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        inputField = new JTextField(ROWS_PER_COLUMN);
        enterButton = createStyledButton(ENTER_BUTTON_NAME, COLOR_BLUE);

        numbersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        sortButton = createStyledButton(SORT_DESC_BUTTON_NAME, COLOR_GREEN);
        resetButton = createStyledButton(RESET_BUTTON_NAME, COLOR_GREEN);

        random = new Random();
        descending = true;
        numbers = new ArrayList<>();
        highlightedIndices = new HashSet<>();

        setTitle(APPLICATION_NAME);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(MAIN_WINDOW_SIZE);
        setLocationRelativeTo(null);

        buildIntroScreen();
        buildSortScreen();

        add(mainPanel);
        setVisible(true);
    }

    private void buildIntroScreen() {
        JPanel introPanel = new JPanel(null);

        JLabel label = new JLabel("Enter number count:");
        label.setBounds(250, 100, 200, 25);

        inputField.setBounds(250, 130, 200, 30);
        enterButton.setBounds(290, 180, 120, 40);

        enterButton.addActionListener(e -> {
            String text = inputField.getText().trim();
            try {
                int count = Integer.parseInt(text);
                if (count <= 0) throw new NumberFormatException();
                generateNumbers(count);
                showSortScreen();
            } catch (NumberFormatException ex) {
                showMessage("Enter a valid positive number.");
            }
        });

        introPanel.add(label);
        introPanel.add(inputField);
        introPanel.add(enterButton);

        mainPanel.add(introPanel, INTRO_LAYOUT_NAME);
    }

    private void buildSortScreen() {
        JPanel sortPanel = new JPanel(null);

        JScrollPane scrollPane = new JScrollPane(numbersPanel);
        scrollPane.setBounds(20, 20, 500, 400);

        sortButton.setBounds(540, 100, 100, 35);
        resetButton.setBounds(540, 160, 100, 35);

        sortButton.addActionListener(e -> {
            descending = !descending;
            sortButton.setText(descending ? SORT_DESC_BUTTON_NAME : SORT_ASC_BUTTON_NAME);
            startSorting();
        });

        resetButton.addActionListener(e -> {
            if (sortTimer != null) {
                sortTimer.stop();
            }
            cardLayout.show(mainPanel, INTRO_LAYOUT_NAME);
        });

        sortPanel.add(scrollPane);
        sortPanel.add(sortButton);
        sortPanel.add(resetButton);

        mainPanel.add(sortPanel, SORT_BUTTON_NAME);
    }

    private void generateNumbers(int count) {
        numbers.clear();
        boolean hasSmall = false;
        while (numbers.size() < count) {
            int value = generateRandomNumber();
            if (value <= MIN_VALUE) hasSmall = true;
            numbers.add(value);
        }
        if (!hasSmall) numbers.set(random.nextInt(count), MIN_VALUE);
        updateNumberButtons();
    }

    private int generateRandomNumber() {
        return random.nextInt(MAX_NUMBER) + 1;
    }

    private void updateNumberButtons() {
        numbersPanel.removeAll();
        JPanel column = createColumnPanel();
        int count = 0;
        int index = 0;

        for (int num : numbers) {
            JButton btn = new JButton(String.valueOf(num));

            if (highlightedIndices.contains(index)) {
                btn.setBackground(Color.ORANGE);
                btn.setForeground(Color.BLACK);
            } else {
                styleBlueButton(btn);
            }

            btn.addActionListener(e -> onNumberClick(Integer.parseInt(btn.getText())));

            column.add(Box.createVerticalStrut(5));
            column.add(btn);

            count++;
            index++;

            if (count == ROWS_PER_COLUMN) {
                numbersPanel.add(column);
                column = createColumnPanel();
                count = 0;
            }
        }

        if (count > 0) {
            numbersPanel.add(column);
        }

        numbersPanel.revalidate();
        numbersPanel.repaint();
    }

    private void onNumberClick(int value) {
        if (value <= MIN_VALUE) {
            generateNumbers(value);
        } else {
            showMessage("Please select a value smaller or equal to " + MIN_VALUE + ".");
        }
    }

    private void showSortScreen() {
        descending = true;
        sortButton.setText(SORT_DESC_BUTTON_NAME);
        updateNumberButtons();
        cardLayout.show(mainPanel, SORT_BUTTON_NAME);
    }

    /**
     * Starts the sorting process with visual animation.
     * Uses a timer to animate each step of the quicksort.
     * Only highlights buttons that were swapped at each step.
     */
    private void startSorting() {
        if (numbers == null || numbers.size() <= 1) return;

        quicksortSteps = new ArrayList<>();
        highlightSteps = new ArrayList<>();
        highlightedIndices.clear();

        int[] arr = numbers.stream().mapToInt(i -> i).toArray();
        quicksortWithTracking(arr, 0, arr.length - 1, descending);

        final int[] stepIndex = {0};
        sortTimer = new Timer(300, e -> {
            if (stepIndex[0] >= quicksortSteps.size()) {
                sortTimer.stop();
                highlightedIndices.clear();
                updateNumberButtons();
                return;
            }

            int[] step = quicksortSteps.get(stepIndex[0]);
            Set<Integer> highlight = highlightSteps.get(stepIndex[0]);

            numbers.clear();
            for (int v : step) numbers.add(v);
            if (!descending) Collections.reverse(numbers);

            highlightedIndices.clear();
            highlightedIndices.addAll(highlight);

            updateNumberButtons();
            stepIndex[0]++;
        });
        sortTimer.start();
    }

    /**
     * Performs a quicksort on the given array while tracking each step and swap.
     * Stores intermediate array states and swap indices for animation.
     *
     * @param arr the array to sort
     * @param low the starting index
     * @param high the ending index
     * @param descending true to sort descending, false for ascending
     */
    private void quicksortWithTracking(int[] arr, int low, int high, boolean descending) {
        if (low < high) {
            int pivotIndex = partitionWithTracking(arr, low, high, descending);
            quicksortSteps.add(arr.clone());
            quicksortWithTracking(arr, low, pivotIndex - 1, descending);
            quicksortWithTracking(arr, pivotIndex + 1, high, descending);
        }
    }

    /**
     * Partitions the array using the Lomuto scheme.
     * Tracks the indices of elements that were swapped for visual feedback.
     *
     * @param arr the array to partition
     * @param low the starting index
     * @param high the pivot index
     * @param descending true to sort descending, false for ascending
     * @return the partition index after pivot placement
     */
    private int partitionWithTracking(int[] arr, int low, int high, boolean descending) {
        int pivot = arr[high];
        int i = low;
        Set<Integer> swappedIndices = new HashSet<>();

        for (int j = low; j < high; j++) {
            if ((descending && arr[j] > pivot) || (!descending && arr[j] < pivot)) {
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
                swappedIndices.add(i);
                swappedIndices.add(j);
                i++;
            }
        }

        int temp = arr[i];
        arr[i] = arr[high];
        arr[high] = temp;
        swappedIndices.add(i);
        swappedIndices.add(high);

        highlightSteps.add(new HashSet<>(swappedIndices));
        return i;
    }

    private JPanel createColumnPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(100, 35));
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        return button;
    }

    private void styleBlueButton(JButton btn) {
        btn.setPreferredSize(NUMBER_BUTTON_SIZE);
        btn.setBackground(COLOR_BLUE);
        btn.setForeground(Color.WHITE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}