// Bridge between Kotlin and JavaScript for message rendering

// Called from Kotlin to add a new message
function addMessage(messageId, role, content, isStreaming) {
    const container = document.getElementById('messages-container');
    
    // Check if message already exists (for streaming updates)
    let messageDiv = document.getElementById(messageId);
    
    if (!messageDiv) {
        // Create new message
        messageDiv = document.createElement('div');
        messageDiv.id = messageId;
        messageDiv.className = `message ${role.toLowerCase()}`;
        
        const header = document.createElement('div');
        header.className = 'message-header';
        
        const icon = document.createElement('span');
        icon.className = 'message-icon';
        icon.textContent = role === 'USER' ? '🧑' : '🤖';
        
        const roleLabel = document.createElement('span');
        roleLabel.textContent = role === 'USER' ? 'You' : 'Kiro';
        
        header.appendChild(icon);
        header.appendChild(roleLabel);
        
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        contentDiv.id = `${messageId}-content`;
        
        messageDiv.appendChild(header);
        messageDiv.appendChild(contentDiv);
        
        container.appendChild(messageDiv);
    }
    
    // Update content
    const contentDiv = document.getElementById(`${messageId}-content`);
    contentDiv.innerHTML = content;
    
    // Add streaming indicator if needed
    if (isStreaming) {
        const indicator = document.createElement('span');
        indicator.className = 'streaming-indicator';
        indicator.id = `${messageId}-streaming`;
        contentDiv.appendChild(indicator);
    } else {
        // Remove streaming indicator if it exists
        const indicator = document.getElementById(`${messageId}-streaming`);
        if (indicator) {
            indicator.remove();
        }
    }
    
    // Process code blocks to add action buttons
    processCodeBlocks(contentDiv);
    
    // Process file links
    processFileLinks(contentDiv);
    
    // Scroll to bottom
    window.scrollTo(0, document.body.scrollHeight);
}

// Called from Kotlin to clear all messages
function clearMessages() {
    const container = document.getElementById('messages-container');
    container.innerHTML = '';
}

// Called from Kotlin to update CSS variables for theme
function updateTheme(cssVariables) {
    const root = document.documentElement;
    for (const [key, value] of Object.entries(cssVariables)) {
        root.style.setProperty(key, value);
    }
}

// Process code blocks to add Copy and Apply buttons
function processCodeBlocks(container) {
    const codeBlocks = container.querySelectorAll('pre > code');
    
    codeBlocks.forEach((codeElement, index) => {
        const pre = codeElement.parentElement;
        
        // Skip if already processed
        if (pre.parentElement.classList.contains('code-block-container')) {
            return;
        }
        
        // Get language from class (e.g., language-kotlin)
        const languageClass = Array.from(codeElement.classList).find(cls => cls.startsWith('language-'));
        const language = languageClass ? languageClass.replace('language-', '') : 'text';
        
        // Apply syntax highlighting if highlight.js is available
        if (window.hljs && language !== 'text') {
            try {
                codeElement.className = `language-${language}`;
                hljs.highlightElement(codeElement);
            } catch (e) {
                console.warn('Failed to highlight code block:', e);
            }
        }
        
        // Wrap in container
        const wrapper = document.createElement('div');
        wrapper.className = 'code-block-container';
        pre.parentElement.insertBefore(wrapper, pre);
        wrapper.appendChild(pre);
        
        // Add class to pre
        pre.className = 'code-block';
        
        // Create action buttons
        const actionsDiv = document.createElement('div');
        actionsDiv.className = 'code-actions';
        
        const copyBtn = document.createElement('button');
        copyBtn.className = 'code-action-btn';
        copyBtn.textContent = '📋 Copy';
        copyBtn.onclick = () => copyCode(codeElement.textContent, copyBtn);
        
        const applyBtn = document.createElement('button');
        applyBtn.className = 'code-action-btn';
        applyBtn.textContent = '📝 Apply to Editor';
        applyBtn.onclick = () => applyToEditor(codeElement.textContent, language);
        
        actionsDiv.appendChild(copyBtn);
        actionsDiv.appendChild(applyBtn);
        
        wrapper.appendChild(actionsDiv);
    });
}

// Process file links to make them clickable
function processFileLinks(container) {
    // Match patterns like: src/Main.kt:42 or src/Main.kt
    const filePathRegex = /\b([a-zA-Z0-9_\-./]+\.(kt|java|py|js|ts|tsx|jsx|go|rs|cpp|c|h|hpp|md|txt|json|xml|yaml|yml)):?(\d+)?\b/g;
    
    const walker = document.createTreeWalker(
        container,
        NodeFilter.SHOW_TEXT,
        null,
        false
    );
    
    const textNodes = [];
    let node;
    while (node = walker.nextNode()) {
        // Skip if already inside a link or code block
        let parent = node.parentElement;
        let skip = false;
        while (parent && parent !== container) {
            if (parent.tagName === 'A' || parent.tagName === 'CODE' || parent.tagName === 'PRE') {
                skip = true;
                break;
            }
            parent = parent.parentElement;
        }
        if (!skip && filePathRegex.test(node.textContent)) {
            textNodes.push(node);
        }
    }
    
    textNodes.forEach(textNode => {
        const text = textNode.textContent;
        const matches = [...text.matchAll(filePathRegex)];
        
        if (matches.length === 0) return;
        
        const fragment = document.createDocumentFragment();
        let lastIndex = 0;
        
        matches.forEach(match => {
            // Add text before match
            if (match.index > lastIndex) {
                fragment.appendChild(document.createTextNode(text.substring(lastIndex, match.index)));
            }
            
            // Create link
            const link = document.createElement('a');
            link.className = 'file-link';
            link.textContent = match[0];
            link.href = '#';
            link.onclick = (e) => {
                e.preventDefault();
                const filePath = match[1];
                const line = match[3] ? parseInt(match[3]) : null;
                openFile(filePath, line);
            };
            
            fragment.appendChild(link);
            lastIndex = match.index + match[0].length;
        });
        
        // Add remaining text
        if (lastIndex < text.length) {
            fragment.appendChild(document.createTextNode(text.substring(lastIndex)));
        }
        
        textNode.parentElement.replaceChild(fragment, textNode);
    });
}

// Copy code to clipboard
function copyCode(code, button) {
    // Call Kotlin bridge
    if (window.kiroJavaBridge) {
        window.kiroJavaBridge.copyToClipboard(code);
        
        // Visual feedback
        const originalText = button.textContent;
        button.textContent = '✓ Copied';
        setTimeout(() => {
            button.textContent = originalText;
        }, 2000);
    }
}

// Apply code to editor
function applyToEditor(code, language) {
    // Call Kotlin bridge
    if (window.kiroJavaBridge) {
        window.kiroJavaBridge.applyToEditor(code, language);
    }
}

// Open file in editor
function openFile(filePath, line) {
    // Call Kotlin bridge
    if (window.kiroJavaBridge) {
        window.kiroJavaBridge.openFile(filePath, line || 0);
    }
}
