// Main JavaScript for E-CALL POC System

document.addEventListener('DOMContentLoaded', () => {
    console.log('E-CALL POC System Initialized');

    // Check API Status
    checkApiStatus();

    // Initialize status cards animation
    initStatusCards();
});

// Check API health status
async function checkApiStatus() {
    try {
        const response = await fetch('/api/health');
        const data = await response.json();

        if (data.status === 'OK') {
            console.log('API is healthy:', data);
            updateStatusIndicator('online');
        } else {
            updateStatusIndicator('offline');
        }
    } catch (error) {
        console.error('API health check failed:', error);
        updateStatusIndicator('offline');
    }
}

// Update status indicator
function updateStatusIndicator(status) {
    const footer = document.querySelector('footer');
    if (footer) {
        const statusText = status === 'online' ? '🟢 시스템 정상' : '🔴 시스템 오프라인';
        const existingIndicator = footer.querySelector('.status-indicator');

        if (existingIndicator) {
            existingIndicator.textContent = statusText;
        } else {
            const indicator = document.createElement('span');
            indicator.className = 'status-indicator';
            indicator.textContent = statusText;
            footer.appendChild(indicator);
        }
    }
}

// Initialize status cards with animation
function initStatusCards() {
    const cards = document.querySelectorAll('.status-card');
    cards.forEach((card, index) => {
        setTimeout(() => {
            card.style.opacity = '0';
            card.style.transform = 'translateY(20px)';
            card.style.transition = 'all 0.5s ease';

            setTimeout(() => {
                card.style.opacity = '1';
                card.style.transform = 'translateY(0)';
            }, 100);
        }, index * 100);
    });
}

// Common utility functions
const utils = {
    // Format date
    formatDate: (date) => {
        return new Intl.DateTimeFormat('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    },

    // Show loading spinner
    showLoading: (element) => {
        element.innerHTML = '<div class="spinner">처리 중...</div>';
    },

    // Show error message
    showError: (element, message) => {
        element.innerHTML = `<div class="error">❌ ${message}</div>`;
    },

    // Show success message
    showSuccess: (element, message) => {
        element.innerHTML = `<div class="success">✅ ${message}</div>`;
    }
};

// Export for use in other scripts
window.ecallUtils = utils;