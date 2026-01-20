// Chat history for context
let chatHistory = [];
let isLoading = false;
let currentSystemPrompt = '';
let currentCollection = '';

// Get collection name from URL hash (e.g., /#my-collection)
function getCollectionFromUrl() {
    const hash = window.location.hash.slice(1); // Remove the '#'
    return hash || null;
}

// Update the UI to show current collection
function updateCollectionDisplay() {
    const collection = getCollectionFromUrl();
    currentCollection = collection;

    const titleElement = document.querySelector('.welcome-message h1');
    const headerElement = document.querySelector('.sidebar-header');

    if (collection) {
        document.title = `Datamine Help - ${collection}`;
        if (titleElement) {
            titleElement.textContent = `Datamine Help`;
            const subtitleElement = document.querySelector('.welcome-message p');
            if (subtitleElement) {
                subtitleElement.textContent = `Collection: ${collection}`;
            }
        }
        // Add collection indicator to sidebar if not exists
        let collectionIndicator = document.getElementById('collection-indicator');
        if (!collectionIndicator && headerElement) {
            collectionIndicator = document.createElement('div');
            collectionIndicator.id = 'collection-indicator';
            collectionIndicator.style.cssText = 'padding: 8px 12px; font-size: 12px; color: var(--text-secondary); border-bottom: 1px solid var(--border-color);';
            collectionIndicator.innerHTML = `<span style="color: var(--accent-color);">Collection:</span> ${collection}`;
            headerElement.parentNode.insertBefore(collectionIndicator, headerElement.nextSibling);
        } else if (collectionIndicator) {
            collectionIndicator.innerHTML = `<span style="color: var(--accent-color);">Collection:</span> ${collection}`;
        }
    } else {
        document.title = 'Datamine Help';
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    updateCollectionDisplay();
    loadSystemPrompt();
    document.getElementById('message-input').focus();
});

// Handle URL hash changes
window.addEventListener('hashchange', () => {
    updateCollectionDisplay();
    newChat(); // Start fresh chat when collection changes
});

// Auto-resize textarea
function autoResize(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px';
}

// Handle keyboard events
function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// Send message
async function sendMessage() {
    const input = document.getElementById('message-input');
    const message = input.value.trim();

    if (!message || isLoading) return;

    // Hide welcome message
    const welcome = document.getElementById('welcome-message');
    if (welcome) {
        welcome.style.display = 'none';
    }

    // Add user message to UI
    addMessage('user', message);

    // Clear input
    input.value = '';
    autoResize(input);

    // Add to history
    chatHistory.push({ role: 'user', content: message });

    // Show loading
    isLoading = true;
    updateSendButton();
    const loadingId = showTypingIndicator();

    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: message,
                history: chatHistory.slice(-10), // Last 10 messages for context
                systemPrompt: currentSystemPrompt || null,
                collectionName: currentCollection || null
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        // Remove loading indicator
        removeTypingIndicator(loadingId);

        // Add assistant message
        addMessage('assistant', data.message, data.sources);

        // Add to history
        chatHistory.push({ role: 'assistant', content: data.message });

    } catch (error) {
        console.error('Error:', error);
        removeTypingIndicator(loadingId);
        showError('Failed to get response. Please check that the server is running and try again.');
    } finally {
        isLoading = false;
        updateSendButton();
    }
}

// Add message to chat
function addMessage(role, content, sources = []) {
    const container = document.getElementById('chat-container');

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${role}`;

    const avatar = role === 'user' ? 'You' : 'Alfred';
    const avatarClass = role === 'user' ? 'user' : 'assistant';

    let sourcesHtml = '';
    const minRelevance = 0.8; // Minimum 30% relevance to show source
    const filteredSources = sources ? sources.filter(s => s.score >= minRelevance) : [];
    if (filteredSources.length > 0) {
        sourcesHtml = `
            <div class="sources">
                <div class="sources-label">Sources:</div>
                ${filteredSources.map(s => {
                    const relevance = Math.round(s.score * 100);
                    return `<a href="${escapeHtml(s.url)}" target="_blank" class="source-link">${escapeHtml(s.title)} <span class="relevance">(${relevance}%)</span></a>`;
                }).join('')}
            </div>
        `;
    }

    messageDiv.innerHTML = `
        <div class="message-content">
            <div class="avatar ${avatarClass}">${avatar}</div>
            <div class="message-text">
                ${formatMessage(content)}
                ${sourcesHtml}
            </div>
        </div>
    `;

    container.appendChild(messageDiv);
    scrollToBottom();
}

// Format message with basic markdown
function formatMessage(text) {
    // Escape HTML first
    let formatted = escapeHtml(text);

    // Code blocks
    formatted = formatted.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>');

    // Inline code
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Bold
    formatted = formatted.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

    // Italic
    formatted = formatted.replace(/\*([^*]+)\*/g, '<em>$1</em>');

    // Parse markdown tables
    formatted = parseMarkdownTables(formatted);

    // Line breaks to paragraphs
    formatted = formatted.split('\n\n').map(p => `<p>${p}</p>`).join('');
    formatted = formatted.replace(/\n/g, '<br>');

    return formatted;
}

// Parse markdown tables into HTML
function parseMarkdownTables(text) {
    const lines = text.split('\n');
    let result = [];
    let tableLines = [];
    let inTable = false;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        const isTableRow = line.startsWith('|') && line.endsWith('|');

        if (isTableRow) {
            if (!inTable) {
                inTable = true;
                tableLines = [];
            }
            tableLines.push(line);
        } else {
            if (inTable) {
                // End of table, convert to HTML
                result.push(convertTableToHtml(tableLines));
                tableLines = [];
                inTable = false;
            }
            result.push(lines[i]);
        }
    }

    // Handle table at end of text
    if (inTable && tableLines.length > 0) {
        result.push(convertTableToHtml(tableLines));
    }

    return result.join('\n');
}

// Convert table lines to HTML table
function convertTableToHtml(tableLines) {
    if (tableLines.length < 2) return tableLines.join('\n');

    let html = '<table class="markdown-table">';

    for (let i = 0; i < tableLines.length; i++) {
        const line = tableLines[i];

        // Skip separator row (contains only |, -, :, and spaces)
        if (/^\|[\s\-:|]+\|$/.test(line)) {
            continue;
        }

        // Parse cells
        const cells = line
            .slice(1, -1) // Remove leading/trailing |
            .split('|')
            .map(cell => cell.trim());

        const tag = i === 0 ? 'th' : 'td';
        const rowClass = i === 0 ? 'table-header' : '';

        html += `<tr class="${rowClass}">`;
        for (const cell of cells) {
            html += `<${tag}>${cell}</${tag}>`;
        }
        html += '</tr>';
    }

    html += '</table>';
    return html;
}

// Escape HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Show typing indicator
function showTypingIndicator() {
    const container = document.getElementById('chat-container');
    const id = 'typing-' + Date.now();

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message assistant';
    messageDiv.id = id;

    messageDiv.innerHTML = `
        <div class="message-content">
            <div class="avatar assistant">Alfred</div>
            <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
            </div>
        </div>
    `;

    container.appendChild(messageDiv);
    scrollToBottom();

    return id;
}

// Remove typing indicator
function removeTypingIndicator(id) {
    const element = document.getElementById(id);
    if (element) {
        element.remove();
    }
}

// Show error message
function showError(message) {
    const container = document.getElementById('chat-container');

    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;

    container.appendChild(errorDiv);
    scrollToBottom();

    // Remove after 5 seconds
    setTimeout(() => errorDiv.remove(), 5000);
}

// Update send button state
function updateSendButton() {
    const btn = document.getElementById('send-btn');
    btn.disabled = isLoading;
}

// Scroll to bottom
function scrollToBottom() {
    const container = document.getElementById('chat-container');
    container.scrollTop = container.scrollHeight;
}

// New chat
function newChat() {
    chatHistory = [];
    const container = document.getElementById('chat-container');
    const collection = getCollectionFromUrl();
    const subtitle = collection
        ? `Collection: ${collection}`
        : 'How can I help you today?';

    container.innerHTML = `
        <div class="welcome-message" id="welcome-message">
            <h1>Datamine Help</h1>
            <p>${subtitle}</p>
        </div>
    `;
    document.getElementById('message-input').focus();
}

// Load system prompt from server
async function loadSystemPrompt() {
    try {
        const response = await fetch('/api/chat/system-prompt');
        const data = await response.json();
        currentSystemPrompt = data.prompt;
        document.getElementById('system-prompt').value = currentSystemPrompt;
    } catch (error) {
        console.error('Error loading system prompt:', error);
    }
}

// Open system prompt dialog
function openSystemPromptDialog() {
    document.getElementById('system-prompt').value = currentSystemPrompt;
    document.getElementById('prompt-dialog').classList.add('open');
}

// Close system prompt dialog
function closeSystemPromptDialog() {
    document.getElementById('prompt-dialog').classList.remove('open');
}

// Close dialog when clicking overlay
function closeDialogOnOverlay(event) {
    if (event.target.classList.contains('dialog-overlay')) {
        closeSystemPromptDialog();
    }
}

// Save system prompt to server
async function saveSystemPrompt() {
    const prompt = document.getElementById('system-prompt').value.trim();

    try {
        const response = await fetch('/api/chat/system-prompt', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ prompt: prompt })
        });

        if (response.ok) {
            currentSystemPrompt = prompt;
            closeSystemPromptDialog();
        } else {
            const data = await response.json();
            alert('Failed to save: ' + (data.error || 'Unknown error'));
        }
    } catch (error) {
        console.error('Error saving system prompt:', error);
        alert('Failed to save system prompt. Please try again.');
    }
}

// Reset system prompt to default
async function resetSystemPrompt() {
    try {
        const response = await fetch('/api/chat/system-prompt', {
            method: 'DELETE'
        });

        if (response.ok) {
            const data = await response.json();
            currentSystemPrompt = data.prompt;
            document.getElementById('system-prompt').value = currentSystemPrompt;
        } else {
            alert('Failed to reset system prompt');
        }
    } catch (error) {
        console.error('Error resetting system prompt:', error);
        alert('Failed to reset system prompt. Please try again.');
    }
}
